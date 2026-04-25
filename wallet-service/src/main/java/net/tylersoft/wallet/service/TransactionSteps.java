package net.tylersoft.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.charge.ChargeValueType;
import net.tylersoft.wallet.common.TransactionStatus;
import net.tylersoft.wallet.gateway.CardChargeRequest;
import net.tylersoft.wallet.gateway.DeviceFingerprintRequest;
import net.tylersoft.wallet.gateway.PaymentGatewayPort;
import net.tylersoft.wallet.model.*;
import net.tylersoft.wallet.repository.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

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
    private final SysServiceRepository sysServiceRepository;

    // ── Step 1: Staging ───────────────────────────────────────────────────────

    /**
     * Inserts a PENDING record into {@code trx_messages} (the staging table).
     * Assigns a unique {@code transactionRef}. All downstream steps reference
     * this record via {@link TransactionContext#getStagedMessage()}.
     */
    private Mono<ServiceManagement> getServiceConfiguration(String coreService, String serviceCode) {
        return sysServiceRepository.findByTransactionType(coreService)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Service not configured")))
                .flatMap(each -> serviceManagementRepository.findByServiceIdAndServiceCode(each.getId(), serviceCode)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Service not configured")))
                        .flatMap(Mono::just));
    }

    public TransactionStep staging() {
        return ctx -> {
            var req = ctx.getRequest();

            TrxMessage msg = new TrxMessage();
            String ref = req.getTransactionRef() != null ? req.getTransactionRef() : UUID.randomUUID().toString();
            msg.setTransactionRef(ref);
            msg.setAmount(BigDecimal.valueOf(req.getAmount()));
            msg.setDebitAccount(req.getDebitAccount());
            msg.setCreditAccount(req.getCreditAccount());
            msg.setCurrency(req.getCurrency());
            msg.setPhoneNumber(req.getPhoneNumber());
            msg.setTransactionType(req.getTransactionType());
            msg.setTransactionCode(req.getTransactionCode());
            msg.setTotalCharge(BigDecimal.ZERO);
            msg.setStatus(TransactionStatus.STARTED.code());
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
            Mono<ServiceManagement> svcMono = getServiceConfiguration(req.getTransactionType(), req.getTransactionCode());
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
     *   <li>Marks the staged {@code trx_messages} row as successful (status=1, status=00).</li>
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
            msg.setStatus(TransactionStatus.FAILED.code());
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

        Account debit = ctx.getDebitAccount();
        String currency = ctx.getRequest().getCurrency();
        OffsetDateTime now = OffsetDateTime.now();

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
        msg.setStatus(TransactionStatus.COMPLETED.code());
        msg.setUpdatedOn(OffsetDateTime.now());
        return trxMessageRepository.save(msg);
    }

    // ── Card topup steps ─────────────────────────────────────────────────────

    /**
     * Validates the card topup transaction. Identical to {@link #validateTransaction()} except:
     * <ul>
     *   <li>No phone-number check on the debit side — debit is a GL/suspense account, not a customer wallet.</li>
     *   <li>Phone-number check is performed against the <em>credit</em> (customer) account.</li>
     *   <li>No dormant/blocked check on the debit side beyond confirming it allows debits.</li>
     * </ul>
     */
    public TransactionStep validateCardTopupTransaction() {
        return ctx -> {
            var req = ctx.getRequest();
            Mono<ServiceManagement> svcMono = getServiceConfiguration(ctx.getRequest().getTransactionType(), ctx.getRequest().getTransactionCode());

            Mono<Account> creditMono = accountRepository.findByAccountNumber(req.getCreditAccount())
                    .switchIfEmpty(Mono.error(new TxException("T02", "Credit account not found: " + req.getCreditAccount())));

            return Mono.zip(creditMono, svcMono)
                    .flatMap(tuple -> {

                        Account credit = tuple.getT1();
                        ServiceManagement svc = tuple.getT2();

                        return accountRepository.findById(svc.getAccountId())
                                .flatMap(debit -> {

                                    if (!Boolean.TRUE.equals(debit.getAllowDr()))
                                        return Mono.just(ctx.withFailure("T06", "GL account does not allow debits"));
                                    if (Boolean.TRUE.equals(credit.getBlocked()))
                                        return Mono.just(ctx.withFailure("T07", "Credit account is blocked"));
                                    if (Boolean.TRUE.equals(credit.getDormant()))
                                        return Mono.just(ctx.withFailure("T11", "Credit account is dormant"));
                                    if (!Boolean.TRUE.equals(credit.getAllowCr()))
                                        return Mono.just(ctx.withFailure("T08", "Credit account does not allow credits"));
                                    if (!req.getPhoneNumber().equals(credit.getPhoneNumber()))
                                        return Mono.just(ctx.withFailure("T09", "Phone number does not match wallet account"));

                                    log.debug("Card topup validated — gl={} credit={} service={}",
                                            debit.getAccountNumber(), credit.getAccountNumber(), svc.getServiceCode());
                                    var staged = ctx.getStagedMessage();
                                    staged.setDebitAccount(debit.getAccountNumber());
                                    ctx.toBuilder().stagedMessage(staged).build();
                                    return trxMessageRepository.save(staged)
                                            .flatMap(updated -> Mono.just(ctx.toBuilder()
                                                    .debitAccount(debit)
                                                    .creditAccount(credit)
                                                    .stagedMessage(updated)
                                                    .serviceManagement(svc)
                                                    .build()));
                                });
                    })
                    .onErrorResume(TxException.class, ex ->
                            Mono.just(ctx.withFailure(ex.code, ex.getMessage())));
        };
    }

    public TransactionStep updateCreditAccount() {
        return ctx -> getServiceConfiguration(ctx.getRequest().getTransactionType(), ctx.getRequest().getTransactionCode())
                .flatMap(cfg -> accountRepository.findById(cfg.getAccountId())
                        .flatMap(account -> Mono.just(ctx.toBuilder().debitAccount(account).build())));
    }

    /**
     * Validates limits for a card topup. Skips the debit-side balance check because
     * the debit account is a GL/suspense account funded externally by the payment gateway.
     * Only the service-level single-transaction limit is enforced.
     */
    public TransactionStep validateCardTopupLimits() {
        return ctx -> {
            ServiceManagement svc = ctx.getServiceManagement();
            BigDecimal amount = BigDecimal.valueOf(ctx.getRequest().getAmount());

            if (exceedsLimit(amount, svc.getDailyLimit())) {
                return Mono.just(ctx.withFailure("L02",
                        "Amount exceeds the configured limit of " + svc.getDailyLimit()));
            }

            log.debug("Card topup limit check passed");
            return Mono.just(ctx);
        };
    }

    /**
     * Calls the payment gateway to initiate the card charge. On a successful initiation
     * the staged {@code trx_messages} row is moved to {@code CALLBACK_WAIT}. If the gateway
     * rejects the charge immediately the context is marked as failed.
     *
     * @param pg the payment gateway adapter to use
     */
    public TransactionStep initiateCardCharge(PaymentGatewayPort pg) {
        return ctx -> {
            var msg = ctx.getStagedMessage();
            var card = ctx.getCardDetails();
            var extras = ctx.getTopupExtras();

            CardChargeRequest chargeReq = new CardChargeRequest(
                    msg.getTransactionRef(),
                    msg.getChannelReference(),
                    card.pan(),
                    card.cvv(),
                    card.expiry(),
                    card.cardType(),
                    ctx.getRequest().getAmount(),
                    ctx.getRequest().getCurrency(),
                    ctx.getRequest().getPhoneNumber(),
                    extras != null ? extras.cardholderName() : null,
                    extras != null ? extras.email() : null,
                    extras != null ? extras.ipAddress() : null,
                    extras != null ? extras.httpAcceptContent() : null,
                    extras != null ? extras.httpBrowserLanguage() : null,
                    extras != null ? extras.httpBrowserJavaEnabled() : null,
                    extras != null ? extras.httpBrowserJavaScriptEnabled() : null,
                    extras != null ? extras.httpBrowserColorDepth() : null,
                    extras != null ? extras.httpBrowserScreenHeight() : null,
                    extras != null ? extras.httpBrowserScreenWidth() : null,
                    extras != null ? extras.httpBrowserTimeDifference() : null,
                    extras != null ? extras.userAgentBrowserValue() : null
            );

            return pg.charge(chargeReq)
                    .flatMap(result -> {
                        if (result.success()) {
                            log.info("PG charge initiated esbRef={} pgRef={}", msg.getId(), result.pgTransactionId());
                            return asyncUpdateStatus(msg.getId(), TransactionStatus.CALLBACK_WAIT,
                                    result.responseCode(), result.responseMessage())
                                    .thenReturn(ctx);
                        }
                        log.warn("PG charge rejected esbRef={} code={}", msg.getId(), result.responseCode());
                        return Mono.just(ctx.withFailure(result.responseCode(), result.responseMessage()));
                    })
                    .onErrorResume(ex -> {
                        log.error("PG charge error for esbRef={}", msg.getId(), ex);
                        return Mono.just(ctx.withFailure("PG01", "Payment gateway error: " + ex.getMessage()));
                    });
        };
    }

    /**
     * Calls the payment gateway device fingerprint endpoint (Phase 1 of 3DS card topup).
     * On success, stores the returned {@code referenceId} in {@code TrxMessage.channelReference}
     * and updates the transaction status to {@link TransactionStatus#DEVICE_PROFILING}.
     */
    public TransactionStep initiateDeviceFingerprint(PaymentGatewayPort pg) {
        return ctx -> {
            var msg = ctx.getStagedMessage();
            DeviceFingerprintRequest req = getDeviceFingerprintRequest(ctx, msg);

            return pg.deviceFingerprint(req)
                    .flatMap(result -> {
                        if (result.success()) {
                            log.info("Device fingerprint success tranid={} referenceId={}",
                                    msg.getTransactionRef(), result.referenceId());
                            msg.setChannelReference(result.referenceId());
                            return trxMessageRepository.save(msg)
                                    .flatMap(saved -> asyncUpdateStatus(saved.getId(),
                                            TransactionStatus.DEVICE_PROFILING,
                                            result.statusCode(), result.message()))
                                    .thenReturn(ctx.toBuilder()
                                            .stagedMessage(msg)
                                            .deviceDataCollectionUrl(result.deviceDataCollectionUrl())
                                            .deviceAccessToken(result.accessToken())
                                            .build());
                        }
                        log.warn("Device fingerprint failed tranid={} code={}",
                                msg.getTransactionRef(), result.statusCode());
                        return Mono.just(ctx.withFailure(result.statusCode(), result.message()));
                    })
                    .onErrorResume(ex -> {
                        log.error("Device fingerprint error tranid={}", msg.getTransactionRef(), ex);
                        return Mono.just(ctx.withFailure("PG01",
                                "Payment gateway error: " + ex.getMessage()));
                    });
        };
    }

    private static DeviceFingerprintRequest getDeviceFingerprintRequest(TransactionContext ctx, TrxMessage msg) {
        var card = ctx.getCardDetails();
        var extras = ctx.getTopupExtras();

        String[] parts = card.expiry().split("/");
        String month = parts[0].trim();
        String year = parts.length > 1 ? parts[1].trim() : "";
        if (year.length() == 2) year = "20" + year;

        String holderName = (extras != null && extras.cardholderName() != null)
                ? extras.cardholderName() : "John Doe";
        String[] names = holderName.split(" ", 2);
        String firstName = names[0];
        String secondName = names.length > 1 ? names[1] : "";

        String phone = ctx.getRequest().getPhoneNumber();
        String localPhone = (phone != null && phone.startsWith("267")) ? phone.substring(3) : phone;

        String amount = String.format("%.2f", ctx.getRequest().getAmount());

        return new DeviceFingerprintRequest(
                msg.getTransactionRef(),
                amount,
                ctx.getRequest().getCurrency(),
                "Botswana",
                firstName,
                secondName,
                localPhone,
                extras != null ? extras.email() : null,
                card.pan(),
                month,
                year,
                card.cvv(),
                card.cardType()
        );
    }

    // ── Async status update ───────────────────────────────────────────────────

    /**
     * Updates the status of a staged transaction outside the normal pipeline —
     * for example when a callback arrives asynchronously after the pipeline has
     * already returned.
     *
     * <p>Uses a targeted UPDATE query so the full entity does not need to be
     * loaded and re-saved. Safe to call fire-and-forget via {@code .subscribe()}.
     *
     * @param esbRef          primary key of the {@code trx_messages} row
     * @param status          new {@link TransactionStatus}
     * @param responseCode    provider/system response code (e.g. "00", "01")
     * @param responseMessage human-readable status description
     * @return {@link Mono} emitting the number of rows affected (1 on success, 0 if not found)
     */
    public Mono<Integer> asyncUpdateStatus(Long esbRef,
                                           TransactionStatus status,
                                           String responseCode,
                                           String responseMessage) {
        log.info("Async status update esbRef={} status={} code={}", esbRef, status, responseCode);
        return trxMessageRepository.updateStatus(esbRef, status.code(), responseCode, responseMessage)
                .doOnSuccess(rows -> {
                    if (rows == 0) {
                        log.warn("asyncUpdateStatus: no row found for esbRef={}", esbRef);
                    } else {
                        log.debug("asyncUpdateStatus: esbRef={} updated to {}", esbRef, status);
                    }
                })
                .doOnError(err -> log.error("asyncUpdateStatus failed for esbRef={}", esbRef, err));
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
