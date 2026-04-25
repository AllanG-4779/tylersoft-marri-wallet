package net.tylersoft.wallet.quote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.airtime.AirtimePurchaseRequest;
import net.tylersoft.wallet.airtime.AirtimeService;
import net.tylersoft.wallet.common.FTRequest;
import net.tylersoft.wallet.model.Account;
import net.tylersoft.wallet.model.ChargeConfig;
import net.tylersoft.wallet.repository.AccountRepository;
import net.tylersoft.wallet.repository.ChargeConfigRepository;
import net.tylersoft.wallet.repository.ServiceManagementRepository;
import net.tylersoft.wallet.repository.SysServiceRepository;
import net.tylersoft.wallet.service.FundTransferService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteService {

    private static final int QUOTE_TTL_MINUTES = 10;

    private final SysServiceRepository sysServiceRepository;
    private final ServiceManagementRepository serviceManagementRepository;
    private final ChargeConfigRepository chargeConfigRepository;
    private final AccountRepository accountRepository;
    private final TransactionQuoteRepository quoteRepository;
    private final FundTransferService fundTransferService;
    private final AirtimeService airtimeService;

    public Mono<QuoteResponse> enquire(Jwt jwt, TransactionEnquiryRequest req) {
        String callerPhone = jwt.getClaimAsString("phone");

        return sysServiceRepository.findByTransactionType(req.transactionType())
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Unknown transaction type: " + req.transactionType())))
                .flatMap(svc -> serviceManagementRepository
                        .findByServiceIdAndServiceCode(svc.getId(), req.transactionCode())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                "Unknown transaction code: " + req.transactionCode()))))
                .flatMap(svcMgmt -> {
                    BigDecimal amount = BigDecimal.valueOf(req.amount());

                    Mono<BigDecimal> feeMono = chargeConfigRepository
                            .findApplicable(svcMgmt.getId(), amount)
                            .collectList()
                            .map(charges -> charges.stream()
                                    .map(c -> computeCharge(c, amount))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add));

                    Mono<Account> debitMono = accountRepository
                            .findByAccountNumber(req.debitAccount())
                            .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                    "Debit account not found: " + req.debitAccount())));

                    Mono<String> recipientNameMono = resolveRecipientName(req);

                    return Mono.zip(feeMono, debitMono, recipientNameMono)
                            .flatMap(tuple -> {
                                BigDecimal fee = tuple.getT1();
                                Account debit = tuple.getT2();
                                String recipientName = tuple.getT3();
                                BigDecimal totalDebit = amount.add(fee);

                                if (!callerPhone.equals(debit.getPhoneNumber())) {
                                    return Mono.error(new SecurityException(
                                            "Phone number does not match debit account"));
                                }
                                if (debit.getAvailableBalance().compareTo(totalDebit) < 0) {
                                    return Mono.error(new IllegalStateException(
                                            "Insufficient balance. Required: " + totalDebit
                                                    + ", Available: " + debit.getAvailableBalance()));
                                }

                                OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(QUOTE_TTL_MINUTES);
                                TransactionQuote quote = new TransactionQuote();
                                quote.setToken(UUID.randomUUID().toString());
                                quote.setTransactionType(req.transactionType());
                                quote.setTransactionCode(req.transactionCode());
                                quote.setDebitAccount(req.debitAccount());
                                quote.setCreditAccount(req.creditAccount());
                                quote.setRecipientPhone(req.recipientPhone());
                                quote.setPhoneNumber(callerPhone);
                                quote.setAmount(amount);
                                quote.setFee(fee);
                                quote.setTotalDebit(totalDebit);
                                quote.setCurrency(req.currency());
                                quote.setRecipientName(recipientName);
                                quote.setStatus("PENDING");
                                quote.setExpiresAt(expiresAt);

                                log.info("Quote created type={} code={} debit={} amount={} fee={}",
                                        req.transactionType(), req.transactionCode(),
                                        req.debitAccount(), amount, fee);

                                return quoteRepository.save(quote)
                                        .map(saved -> new QuoteResponse(
                                                saved.getToken(),
                                                saved.getTransactionType(),
                                                saved.getTransactionCode(),
                                                amount,
                                                fee,
                                                totalDebit,
                                                req.currency(),
                                                recipientName,
                                                req.creditAccount(),
                                                req.recipientPhone(),
                                                expiresAt.toString(),
                                                buildSummary(req, fee, totalDebit, recipientName)));
                            });
                });
    }

    public Mono<ConfirmResponse> confirm(Jwt jwt, ConfirmRequest req) {
        String callerPhone = jwt.getClaimAsString("phone");

        return quoteRepository.findByToken(req.quoteToken())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid or unknown quote token")))
                .flatMap(quote -> {
                    if (!"PENDING".equals(quote.getStatus())) {
                        String reason = "CONSUMED".equals(quote.getStatus())
                                ? "Quote has already been used" : "Quote has expired";
                        return Mono.error(new IllegalStateException(reason));
                    }
                    if (OffsetDateTime.now().isAfter(quote.getExpiresAt())) {
                        quote.setStatus("EXPIRED");
                        return quoteRepository.save(quote)
                                .then(Mono.error(new IllegalStateException("Quote has expired. Please re-enquire.")));
                    }
                    if (!callerPhone.equals(quote.getPhoneNumber())) {
                        return Mono.error(new SecurityException("Quote does not belong to this caller"));
                    }

                    return revalidateFees(quote)
                            .flatMap(feeChanged -> {
                                if (feeChanged) {
                                    return Mono.error(new IllegalStateException(
                                            "Transaction charges have changed. Please re-enquire."));
                                }
                                return executeTransaction(quote)
                                        .flatMap(result -> markConsumed(quote)
                                                .thenReturn(result));
                            });
                });
    }

    private Mono<String> resolveRecipientName(TransactionEnquiryRequest req) {
        if ("FT".equals(req.transactionType()) && req.creditAccount() != null) {
            return accountRepository.findByAccountNumber(req.creditAccount())
                    .map(a -> a.getAccountName() != null ? a.getAccountName() : "Unknown")
                    .defaultIfEmpty("Unknown");
        }
        return Mono.just("");
    }

    private Mono<Boolean> revalidateFees(TransactionQuote quote) {
        return sysServiceRepository.findByTransactionType(quote.getTransactionType())
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Service no longer available: " + quote.getTransactionType())))
                .flatMap(svc -> serviceManagementRepository
                        .findByServiceIdAndServiceCode(svc.getId(), quote.getTransactionCode())
                        .switchIfEmpty(Mono.error(new IllegalStateException(
                                "Service code no longer available: " + quote.getTransactionCode()))))
                .flatMap(svcMgmt -> chargeConfigRepository
                        .findApplicable(svcMgmt.getId(), quote.getAmount())
                        .collectList()
                        .map(charges -> {
                            BigDecimal currentFee = charges.stream()
                                    .map(c -> computeCharge(c, quote.getAmount()))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            boolean changed = currentFee.compareTo(quote.getFee()) != 0;
                            if (changed) {
                                log.warn("Fee changed at confirm time token={} original={} current={}",
                                        quote.getToken(), quote.getFee(), currentFee);
                            }
                            return changed;
                        }));
    }

    private Mono<ConfirmResponse> executeTransaction(TransactionQuote quote) {
        return switch (quote.getTransactionType()) {
            case "FT" -> {
                FTRequest ftReq = FTRequest.builder()
                        .debitAccount(quote.getDebitAccount())
                        .creditAccount(quote.getCreditAccount())
                        .amount(quote.getAmount().doubleValue())
                        .currency(quote.getCurrency())
                        .phoneNumber(quote.getPhoneNumber())
                        .transactionType("FT")
                        .transactionCode("FT")
                        .build();
                yield fundTransferService.execute(ftReq)
                        .map(ctx -> ctx.isSuccessful()
                                ? new ConfirmResponse(
                                        ctx.getStagedMessage().getTransactionRef(),
                                        ctx.getStagedMessage().getReceiptNumber(),
                                        "COMPLETED", "Transaction successful")
                                : new ConfirmResponse(null, null, "FAILED",
                                        ctx.getFailureCode() + " - " + ctx.getFailureMessage()));
            }
            case "AIRTIME" -> {
                AirtimePurchaseRequest airtimeReq = new AirtimePurchaseRequest(
                        quote.getDebitAccount(),
                        quote.getRecipientPhone(),
                        quote.getTransactionCode(),
                        quote.getAmount().doubleValue(),
                        quote.getCurrency(),
                        quote.getPhoneNumber()
                );
                yield airtimeService.purchase(airtimeReq)
                        .map(resp -> new ConfirmResponse(
                                resp.reference(),
                                resp.providerReference(),
                                resp.status(),
                                resp.message()));
            }
            default -> Mono.error(new IllegalArgumentException(
                    "Unsupported transaction type: " + quote.getTransactionType()));
        };
    }

    private Mono<Void> markConsumed(TransactionQuote quote) {
        quote.setStatus("CONSUMED");
        return quoteRepository.save(quote).then();
    }

    private BigDecimal computeCharge(ChargeConfig charge, BigDecimal amount) {
        return switch (charge.getValueType()) {
            case FIXED -> charge.getChargeValue();
            case PERCENTAGE -> amount
                    .multiply(charge.getChargeValue())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        };
    }

    private String buildSummary(TransactionEnquiryRequest req, BigDecimal fee,
                                BigDecimal totalDebit, String recipientName) {
        return switch (req.transactionType()) {
            case "FT" -> String.format("Send %s %.2f to %s (%s). Fee: %s. Total deducted: %s.",
                    req.currency(), req.amount(), recipientName, req.creditAccount(), fee, totalDebit);
            case "AIRTIME" -> String.format("Buy %s %.2f %s airtime for %s. Fee: %s. Total deducted: %s.",
                    req.currency(), req.amount(), req.transactionCode(), req.recipientPhone(), fee, totalDebit);
            default -> String.format("%s %s %.2f. Fee: %s. Total: %s.",
                    req.transactionType(), req.currency(), req.amount(), fee, totalDebit);
        };
    }
}
