package net.tylersoft.wallet.topup;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.common.FTRequest;
import net.tylersoft.wallet.common.TransactionStatus;
import net.tylersoft.wallet.gateway.CardChargeRequest;
import net.tylersoft.wallet.gateway.PaymentGatewayPort;
import net.tylersoft.wallet.repository.*;
import net.tylersoft.wallet.service.TransactionContext;
import net.tylersoft.wallet.service.TransactionPipeline;
import net.tylersoft.wallet.service.TransactionSteps;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardTopupService {

    private final TransactionSteps steps;
    private final PaymentGatewayPort paymentGateway;
    private final ServiceManagementRepository serviceManagementRepository;
    private final AccountRepository accountRepository;
    private final SysServiceRepository serviceRepository;
    private final ChargeConfigRepository chargeConfigRepository;
    private final TrxMessageRepository trxMessageRepository;

    private TransactionPipeline profilePipeline;

    @PostConstruct
    void init() {
        profilePipeline = TransactionPipeline.builder()
//                .step(steps.updateCreditAccount())
                .step(steps.staging())
                .step(steps.validateCardTopupTransaction())
//                .step(steps.upd)
                .step(steps.validateCardTopupLimits())
                .step(steps.initiateDeviceFingerprint(paymentGateway))
                .onFailure(steps.markFailed())
                .build();
    }

    /**
     * Phase 1 — device profiling.
     * <p>
     * Validates the transaction, stages a record, calls the payment gateway device
     * fingerprint endpoint, and saves the returned {@code referenceId} in
     * {@code TrxMessage.channelReference} with status {@code DEVICE_PROFILING}.
     * <p>
     * The client must use the returned {@code esbRef} and {@code deviceDataCollectionUrl}
     * to complete 3DS browser-based device collection, then call {@link #initiate}.
     */
    public Mono<CardProfileResponse> profile(CardProfileRequest request) {
        FTRequest ftReq = FTRequest.builder()
                .creditAccount(request.creditAccount())
                .amount(request.amount())
                .currency(request.currency())
                .transactionType("DEPOSIT")
                .transactionCode("CARD")
                .phoneNumber(request.phoneNumber())
                .transactionRef(request.tranid())
                .build();

        TransactionContext initialCtx = TransactionContext.builder()
                .request(ftReq)
                .totalCharge(BigDecimal.ZERO)
                .failed(false)
                .cardDetails(request.card())
                .topupExtras(new CardTopupExtras(
                        request.cardholderName(),
                        request.email(),
                        null, null, null, null, null, null, null, null, null, null
                ))
                .build();

        log.info("Card profile (device fingerprint) credit={} amount={}", request.creditAccount(), request.amount());

        return profilePipeline.run(initialCtx)
                .map(ctx -> {
                    if (ctx.isSuccessful()) {
                        return new CardProfileResponse(
                                ctx.getStagedMessage().getTransactionRef(),
                                ctx.getDeviceDataCollectionUrl(),
                                ctx.getDeviceAccessToken(),
                                TransactionStatus.DEVICE_PROFILING.name(),
                                "Device profiling successful. Complete 3DS collection then call /initiate."
                        );
                    }
                    return new CardProfileResponse(
                            null, null, null,
                            TransactionStatus.FAILED.name(),
                            ctx.getFailureCode() + " - " + ctx.getFailureMessage()
                    );
                });
    }

    /**
     * Phase 2 — initiates the card charge.
     * <p>
     * Loads the staged transaction by {@code esbRef}, reads the device fingerprint
     * {@code referenceId} from {@code TrxMessage.channelReference}, then calls the
     * payment gateway charge endpoint. On acceptance the status moves to
     * {@code CALLBACK_WAIT}; the transaction is fully posted when the callback arrives.
     */
    public Mono<CardTopupInitiateResponse> initiate(CardTopupPaymentRequest request) {
        return trxMessageRepository.findByTransactionRef(request.esbRef())
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Transaction not found: " + request.esbRef())))
                .flatMap(msg -> {
                    if (msg.getStatus() == null || msg.getStatus() != TransactionStatus.DEVICE_PROFILING.code()) {
                        return Mono.error(new IllegalStateException(
                                "Transaction is not in DEVICE_PROFILING state: " + request.esbRef()));
                    }

                    CardChargeRequest chargeReq = new CardChargeRequest(
                            msg.getTransactionRef(),   // esbRef / tranid
                            msg.getChannelReference(), // referenceId from device fingerprint
                            request.card().pan(),
                            request.card().cvv(),
                            request.card().expiry(),
                            request.card().cardType(),
                            msg.getAmount().doubleValue(),
                            msg.getCurrency(),
                            msg.getPhoneNumber(),
                            null,                      // cardholderName — gateway defaults to "Wallet User"
                            null,                      // email         — gateway defaults to phone@wallet.local
                            request.ipAddress(),
                            request.httpAcceptContent(),
                            request.httpBrowserLanguage(),
                            request.httpBrowserJavaEnabled(),
                            request.httpBrowserJavaScriptEnabled(),
                            request.httpBrowserColorDepth(),
                            request.httpBrowserScreenHeight(),
                            request.httpBrowserScreenWidth(),
                            request.httpBrowserTimeDifference(),
                            request.userAgentBrowserValue()
                    );

                    log.info("Card charge initiate esbRef={} referenceId={} payload={}", msg.getTransactionRef(), msg.getChannelReference(), chargeReq);

                    return paymentGateway.charge(chargeReq)
                            .flatMap(result -> {
                                if (result.success()) {
                                    return steps.asyncUpdateStatus(msg.getId(),
                                                    TransactionStatus.CALLBACK_WAIT,
                                                    result.responseCode(), result.responseMessage())
                                            .thenReturn(new CardTopupInitiateResponse(
                                                    String.valueOf(msg.getId()),
                                                    msg.getTransactionRef(),
                                                    TransactionStatus.CALLBACK_WAIT.name(),
                                                    "Card charge initiated. Awaiting payment gateway callback."
                                            ));
                                }
                                log.warn("PG charge rejected esbRef={} code={}", msg.getTransactionRef(), result.responseCode());
                                return steps.asyncUpdateStatus(msg.getId(),
                                                TransactionStatus.FAILED,
                                                result.responseCode(), result.responseMessage())
                                        .thenReturn(new CardTopupInitiateResponse(
                                                null, null,
                                                TransactionStatus.FAILED.name(),
                                                result.responseCode() + " - " + result.responseMessage()
                                        ));
                            })
                            .onErrorResume(ex -> {
                                log.error("Card charge error esbRef={}", msg.getTransactionRef(), ex);
                                return steps.asyncUpdateStatus(msg.getId(),
                                                TransactionStatus.FAILED, "PG01",
                                                "Payment gateway error: " + ex.getMessage())
                                        .thenReturn(new CardTopupInitiateResponse(
                                                null, null,
                                                TransactionStatus.FAILED.name(),
                                                "PG01 - Payment gateway error"
                                        ));
                            });
                });
    }

    /**
     * Phase 3 — handles the payment gateway callback.
     * <p>
     * On a successful callback the transaction is fully posted (DR GL, CR customer wallet).
     * On a failure callback the staged record is marked {@code FAILED}.
     */
    public Mono<Void> handleCallback(CardTopupCallbackRequest callbackRequest) {
        String tranid = callbackRequest.esbRef();
        boolean pgSuccess = "100".equals(callbackRequest.responseCode());
        return trxMessageRepository.findByTransactionRef(tranid)
                .filter(each -> TransactionStatus.CALLBACK_WAIT.code() == each.getStatus())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Transaction not found or not in CALLBACK_WAIT state: " + tranid)))
                .flatMap(msg -> {
                    if (!pgSuccess) {
                        log.warn("Card topup callback FAILED tranid={} code={}", tranid, callbackRequest.responseCode());
                        return steps.asyncUpdateStatus(msg.getId(), TransactionStatus.FAILED,
                                        callbackRequest.responseCode(), callbackRequest.responseMessage())
                                .then(Mono.empty());
                    }
                    log.info("Card topup callback SUCCESS tranid={} receipt={}", tranid, callbackRequest.receiptNumber());
                    msg.setReceiptNumber(callbackRequest.receiptNumber());
                    return trxMessageRepository.save(msg)
                            .flatMap(this::reconstructContext)
                            .flatMap(ctx -> TransactionPipeline.builder()
                                    .step(steps.post())
                                    .onFailure(steps.markFailed())
                                    .build()
                                    .run(ctx));
                })
                .then();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Mono<TransactionContext> reconstructContext(net.tylersoft.wallet.model.TrxMessage msg) {
        FTRequest req = FTRequest.builder()
                .debitAccount(msg.getDebitAccount())
                .creditAccount(msg.getCreditAccount())
                .amount(msg.getAmount().doubleValue())
                .currency(msg.getCurrency())
                .transactionType(msg.getTransactionType())
                .phoneNumber(msg.getPhoneNumber())
                .build();

        return Mono.zip(
                accountRepository.findByAccountNumber(msg.getDebitAccount())
                        .switchIfEmpty(Mono.error(new IllegalStateException(
                                "Debit account not found during callback post: " + msg.getDebitAccount()))),
                accountRepository.findByAccountNumber(msg.getCreditAccount())
                        .switchIfEmpty(Mono.error(new IllegalStateException(
                                "Credit account not found during callback post: " + msg.getCreditAccount()))),
                serviceManagementRepository.findByServiceCode(msg.getTransactionCode())
                        .switchIfEmpty(Mono.error(new IllegalStateException(
                                "Service not found during callback post: " + msg.getTransactionType())))
        ).flatMap(tuple -> {
            Integer serviceId = tuple.getT3().getId();
            BigDecimal amount = msg.getAmount();
            return chargeConfigRepository.findApplicable(serviceId, amount)
                    .collectList()
                    .map(charges -> TransactionContext.builder()
                            .request(req)
                            .stagedMessage(msg)
                            .debitAccount(tuple.getT1())
                            .creditAccount(tuple.getT2())
                            .serviceManagement(tuple.getT3())
                            .chargeConfigs(charges)
                            .totalCharge(msg.getTotalCharge() != null ? msg.getTotalCharge() : BigDecimal.ZERO)
                            .failed(false)
                            .build());
        });
    }
}
