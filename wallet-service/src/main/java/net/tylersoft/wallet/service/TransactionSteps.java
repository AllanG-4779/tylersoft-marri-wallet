package net.tylersoft.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.charge.ChargeValueType;
import net.tylersoft.wallet.model.Account;
import net.tylersoft.wallet.model.ChargeConfig;
import net.tylersoft.wallet.model.ServiceManagement;
import net.tylersoft.wallet.model.TransactionCharge;
import net.tylersoft.wallet.model.TransactionEntry;
import net.tylersoft.wallet.model.TrxMessage;
import net.tylersoft.wallet.repository.AccountRepository;
import net.tylersoft.wallet.repository.ChargeConfigRepository;
import net.tylersoft.wallet.repository.ServiceManagementRepository;
import net.tylersoft.wallet.repository.TransactionChargeRepository;
import net.tylersoft.wallet.repository.TransactionEntryRepository;
import net.tylersoft.wallet.repository.TrxMessageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Provides the five standard steps of a wallet transaction pipeline.
 *
 * <p>Each method returns a {@link TransactionStep} — a reactive function that
 * receives a {@link TransactionContext}, performs its work, and returns an
 * enriched context. Steps are composed into a {@link TransactionPipeline}:
 *
 * <pre>{@code
 * TransactionPipeline pipeline = TransactionPipeline.builder()
 *         .step(steps.staging())
 *         .step(steps.validateTransaction())
 *         .step(steps.validateCharges())
 *         .step(steps.validateLimits())
 *         .step(steps.post())
 *         .build();
 * }</pre>
 * <p>
 * Different transaction types (FT, bill payment, merchant payment) compose
 * different subsets or orderings of these steps.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionSteps {

    private final TrxMessageRepository trxMessageRepository;
    private final AccountRepository accountRepository;
    private final ServiceManagementRepository serviceManagementRepository;
    private final ChargeConfigRepository chargeConfigRepository;
    private final TransactionEntryRepository transactionEntryRepository;
    private final TransactionChargeRepository transactionChargeRepository;
    private final TransactionalOperator transactionalOperator;

    // ── Step 1: Staging ───────────────────────────────────────────────────────

    /**
     * Inserts a PENDING record into {@code trx_messages} (the staging table).
     * Assigns a unique {@code transactionRef}. All downstream steps reference
     * this record via {@link TransactionContext#getStagedMessage()}.
     */
    public TransactionStep staging() {
        return ctx -> {
            var req = ctx.getRequest();

            TrxMessage msg = new TrxMessage();
            msg.setTransactionRef(UUID.randomUUID().toString());
            msg.setAmount(BigDecimal.valueOf(req.getAmount()));
            msg.setDebitAccount(req.getDebitAccount());
            msg.setCreditAccount(req.getCreditAccount());
            msg.setCurrency(req.getCurrency());
            msg.setPhoneNumber(req.getPhoneNumber());
            msg.setTransactionType(req.getTransactionType());
            msg.setTotalCharge(BigDecimal.ZERO);
            msg.setStatus((short) 0); // 0 = PENDING
            msg.setCreatedOn(OffsetDateTime.now());
            msg.setUpdatedOn(OffsetDateTime.now());
            return trxMessageRepository.save(msg)
                    .map(saved -> {
                        log.debug("Staged trx ref={} amount={}", saved.getTransactionRef(), saved.getAmount());
                        return ctx.toBuilder().stagedMessage(saved).build();
                    });
        };
    }

    // ── Step 2: Validate transaction ──────────────────────────────────────────

    /**
     * Resolves debit account, credit account, and service configuration in
     * parallel. Validates that accounts are active and permit the requested
     * debit/credit operation.
     */
    public TransactionStep validateTransaction() {
        return ctx -> {
            var req = ctx.getRequest();
            Mono<ServiceManagement> svcMono = serviceManagementRepository.findByServiceCode(req.getTransactionType())
                    .switchIfEmpty(Mono.error(new TxException("T03", "No service configuration for type: " + req.getTransactionType())));
            Mono<Account> debitMono = accountRepository.findByAccountNumber(req.getDebitAccount())
                    .switchIfEmpty(Mono.error(new TxException("T01", "Debit account not found: " + req.getDebitAccount())));

            Mono<Account> creditMono = accountRepository.findByAccountNumber(req.getCreditAccount())
                    .switchIfEmpty(Mono.error(new TxException("T02", "Credit account not found: " + req.getCreditAccount())));


            return Mono.zip(debitMono, creditMono, svcMono)
                    .flatMap(tuple -> {
                        ServiceManagement svc = tuple.getT3();
                        Account debit = tuple.getT1();
                        Account credit = tuple.getT2();


                        if (Boolean.TRUE.equals(debit.getBlocked()))
                            return Mono.just(ctx.withFailure("T04", "Debit account is blocked"));
                        if (Boolean.TRUE.equals(debit.getDormant()))
                            return Mono.just(ctx.withFailure("T05", "Debit account is dormant"));
                        if (!Boolean.TRUE.equals(debit.getAllowDr()))
                            return Mono.just(ctx.withFailure("T06", "Debit account does not allow debits"));
                        if (Boolean.TRUE.equals(credit.getBlocked()))
                            return Mono.just(ctx.withFailure("T07", "Credit account is blocked"));
                        if (!Boolean.TRUE.equals(credit.getAllowCr()))
                            return Mono.just(ctx.withFailure("T08", "Credit account does not allow credits"));

                        log.debug("Transaction validated — debit={} credit={} service={}",
                                debit.getAccountNumber(), credit.getAccountNumber(), svc.getServiceCode());
                        if (!ctx.getRequest().getPhoneNumber().equals(debit.getPhoneNumber())) {
                            return Mono.just(ctx.withFailure("T09", "Phone number does not match debit account"));
                        }
                        if (ctx.getRequest().getDebitAccount().equals(ctx.getRequest().getCreditAccount())) {
                            return Mono.just(ctx.withFailure("T10", "Debit and credit accounts cannot be the same"));
                        }
                        return Mono.just(ctx.toBuilder()
                                .debitAccount(debit)
                                .creditAccount(credit)
                                .serviceManagement(svc)
                                .build());
                    })
                    .onErrorResume(TxException.class, ex ->
                            Mono.just(ctx.withFailure(ex.code, ex.getMessage())));
        };
    }

    // ── Step 3: Validate charges ──────────────────────────────────────────────

    /**
     * Loads charge configs applicable to the service + amount band, computes
     * the total charge (FIXED or PERCENTAGE), and enriches the context.
     * Transactions with no configured charges proceed with {@code totalCharge = 0}.
     */
    public TransactionStep validateCharges() {
        return ctx -> {
            BigDecimal amount = BigDecimal.valueOf(ctx.getRequest().getAmount());
            Integer serviceManagementId = ctx.getServiceManagement().getId();

            return chargeConfigRepository.findApplicable(serviceManagementId, amount)
                    .collectList()
                    .map(charges -> {
                        BigDecimal totalCharge = charges.stream()
                                .map(c -> computeCharge(c, amount))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        log.debug("{} charge(s) applicable, total={}", charges.size(), totalCharge);

                        return ctx.toBuilder()
                                .chargeConfigs(charges)
                                .totalCharge(totalCharge)
                                .build();
                    });
        };
    }

    // ── Step 4: Validate limits ───────────────────────────────────────────────

    /**
     * Checks:
     * <ol>
     *   <li>Debit account has sufficient available balance to cover amount + charges.</li>
     *   <li>Transaction amount does not breach the service-level single-transaction limit.</li>
     * </ol>
     *
     * <p>TODO: accumulate per-account daily / weekly / monthly spend from
     * {@code trx_messages} and compare against
     * {@link ServiceManagement#getDailyLimit()}, {@link ServiceManagement#getWeeklyLimit()},
     * {@link ServiceManagement#getMonthlyLimit()}.
     */
    public TransactionStep validateLimits() {
        return ctx -> {
            Account debit = ctx.getDebitAccount();
            ServiceManagement svc = ctx.getServiceManagement();
            BigDecimal amount = BigDecimal.valueOf(ctx.getRequest().getAmount());
            BigDecimal totalRequired = amount.add(ctx.getTotalCharge());

            if (debit.getAvailableBalance().compareTo(totalRequired) < 0) {
                return Mono.just(ctx.withFailure("L01",
                        "Insufficient balance. Required: " + totalRequired
                                + ", Available: " + debit.getAvailableBalance()));
            }

            if (exceedsLimit(amount, svc.getDailyLimit())) {
                return Mono.just(ctx.withFailure("L02",
                        "Amount exceeds the configured limit of " + svc.getDailyLimit()));
            }

            log.debug("Limit check passed for account={}", debit.getAccountNumber());
            return Mono.just(ctx);
        };
    }

    // ── Step 5: Post ──────────────────────────────────────────────────────────

    /**
     * Atomically (within a reactive transaction):
     * <ol>
     *   <li>Writes DR + CR entries to {@code trx_transaction_entries}.</li>
     *   <li>Writes one row per charge to {@code trx_transaction_charges}.</li>
     *   <li>Updates available and actual balances on both accounts.</li>
     *   <li>Marks the staged {@code trx_messages} row as successful (status=1, responseCode=00).</li>
     * </ol>
     */
    public TransactionStep post() {
        return ctx -> transactionalOperator.transactional(doPost(ctx));
    }

    private Mono<TransactionContext> doPost(TransactionContext ctx) {
        Account debit = ctx.getDebitAccount();
        Account credit = ctx.getCreditAccount();
        BigDecimal amount = BigDecimal.valueOf(ctx.getRequest().getAmount());
        BigDecimal totalCharge = ctx.getTotalCharge();
        Long esbRef = ctx.getStagedMessage().getId();
        String currency = ctx.getRequest().getCurrency();
        ServiceManagement svc = ctx.getServiceManagement();
        OffsetDateTime now = OffsetDateTime.now();

        TransactionEntry drEntry = buildEntry(esbRef,
                debit.getAccountNumber(),
                debit.getActualBalance(), debit.getAvailableBalance(),
                amount.add(totalCharge), "DR",
                svc.getSenderNarration(), currency, now);

        TransactionEntry crEntry = buildEntry(esbRef,
                credit.getAccountNumber(),
                credit.getActualBalance(), credit.getAvailableBalance(),
                amount, "CR",
                svc.getReceiverNarration(), currency, now);

        return transactionEntryRepository.save(drEntry)
                .then(transactionEntryRepository.save(crEntry))
                .then(saveChargeEntries(ctx, esbRef, amount))
                .then(debitBalance(debit, amount.add(totalCharge)))
                .then(creditBalance(credit, amount))
                .then(markSuccessful(ctx, totalCharge))
                .thenReturn(ctx.toBuilder()
                        .entries(List.of(drEntry, crEntry))
                        .build());
    }

    // ── Failure handler ───────────────────────────────────────────────────────

    /**
     * Registered via {@code TransactionPipeline.Builder.onFailure(...)}.
     * Updates the staged {@code trx_messages} record with the failure code and
     * message so the row never stays stuck at status=0 (PENDING).
     *
     * <p>Safe to call even if staging itself failed — skips the DB write when
     * {@code stagedMessage} is null.
     */
    public TransactionStep markFailed() {
        return ctx -> {
            if (ctx.getStagedMessage() == null) return Mono.just(ctx);

            TrxMessage msg = ctx.getStagedMessage();
            msg.setResponseCode(ctx.getFailureCode());
            msg.setResponseMessage(ctx.getFailureMessage());
            msg.setStatus((short) 2); // 2 = FAILED
            msg.setUpdatedOn(OffsetDateTime.now());

            log.warn("Transaction failed ref={} code={} reason={}",
                    msg.getTransactionRef(), ctx.getFailureCode(), ctx.getFailureMessage());

            return trxMessageRepository.save(msg).thenReturn(ctx);
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal computeCharge(ChargeConfig charge, BigDecimal amount) {
        return switch (charge.getValueType()) {
            case FIXED -> charge.getChargeValue();
            case PERCENTAGE -> amount
                    .multiply(charge.getChargeValue())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        };
    }

    private boolean exceedsLimit(BigDecimal amount, BigDecimal limit) {
        return limit != null
                && limit.compareTo(BigDecimal.ZERO) > 0
                && amount.compareTo(limit) > 0;
    }

    private TransactionEntry buildEntry(Long esbRef, String accountNumber,
                                        BigDecimal actualBefore, BigDecimal availableBefore,
                                        BigDecimal amount, String drCr,
                                        String narration, String currency, OffsetDateTime now) {
        boolean isDebit = "DR".equals(drCr);

        TransactionEntry entry = new TransactionEntry();
        entry.setEsbRef(esbRef);
        entry.setAccountNumber(accountNumber);
        entry.setActualBalanceBefore(actualBefore);
        entry.setAvailableBalanceBefore(availableBefore);
        entry.setAmount(amount);
        entry.setActualBalanceAfter(isDebit
                ? actualBefore.subtract(amount)
                : actualBefore.add(amount));
        entry.setAvailableBalanceAfter(isDebit
                ? availableBefore.subtract(amount)
                : availableBefore.add(amount));
        entry.setDrCr(drCr);
        entry.setNarration(narration);
        entry.setCurrency(currency);
        entry.setIsBalanceUpdated(true);
        entry.setReversed(false);
        entry.setStatus((short) 1);
        entry.setCreatedOn(now);
        entry.setUpdatedOn(now);
        return entry;
    }

    private Mono<Void> saveChargeEntries(TransactionContext ctx, Long esbRef, BigDecimal amount) {
        List<ChargeConfig> configs = ctx.getChargeConfigs();
        if (configs == null || configs.isEmpty()) return Mono.empty();

        Account        debit    = ctx.getDebitAccount();
        String         currency = ctx.getRequest().getCurrency();
        OffsetDateTime now      = OffsetDateTime.now();

        return Flux.fromIterable(configs)
                .concatMap(cfg -> {
                    BigDecimal chargeAmount = computeCharge(cfg, amount);

                    return accountRepository.findById(cfg.getAccountId())
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "Charge float account not found for chargeId=" + cfg.getId())))
                            .flatMap(floatAccount -> {
                                TransactionCharge charge = new TransactionCharge();
                                charge.setEsbRef(esbRef);
                                charge.setChargeId(cfg.getId());
                                charge.setChargeType(cfg.getChargeType().name());
                                charge.setChargeValue(cfg.getChargeValue());
                                charge.setAmount(amount);
                                charge.setTotalCharge(chargeAmount);
                                charge.setStatusCode("00");
                                charge.setStatusMessage("Success");

                                TransactionEntry drChargeEntry = buildEntry(esbRef,
                                        debit.getAccountNumber(),
                                        debit.getActualBalance(), debit.getAvailableBalance(),
                                        chargeAmount, "DR",
                                        cfg.getSenderNarration(), currency, now);

                                TransactionEntry crChargeEntry = buildEntry(esbRef,
                                        floatAccount.getAccountNumber(),
                                        floatAccount.getActualBalance(), floatAccount.getAvailableBalance(),
                                        chargeAmount, "CR",
                                        cfg.getReceiverNarration(), currency, now);

                                log.debug("Charge ref={} chargeId={} amount={} float={}",
                                        esbRef, cfg.getId(), chargeAmount, floatAccount.getAccountNumber());

                                return transactionChargeRepository.save(charge)
                                        .then(transactionEntryRepository.save(drChargeEntry))
                                        .then(transactionEntryRepository.save(crChargeEntry))
                                        .then(creditBalance(floatAccount, chargeAmount));
                            });
                })
                .then();
    }

    private Mono<Account> debitBalance(Account account, BigDecimal amount) {
        account.setActualBalance(account.getActualBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setUpdatedOn(OffsetDateTime.now());
        return accountRepository.save(account);
    }

    private Mono<Account> creditBalance(Account account, BigDecimal amount) {
        account.setActualBalance(account.getActualBalance().add(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setUpdatedOn(OffsetDateTime.now());
        return accountRepository.save(account);
    }

    private Mono<TrxMessage> markSuccessful(TransactionContext ctx, BigDecimal totalCharge) {
        TrxMessage msg = ctx.getStagedMessage();
        msg.setTotalCharge(totalCharge);
        msg.setResponseCode("00");
        msg.setResponseMessage("Transaction successful");
        msg.setStatus((short) 1); // 1 = SUCCESS
        msg.setUpdatedOn(OffsetDateTime.now());
        return trxMessageRepository.save(msg);
    }

    // ── Private exception — carries a step-specific status code ───────────────

    private static final class TxException extends RuntimeException {
        final String code;

        TxException(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
