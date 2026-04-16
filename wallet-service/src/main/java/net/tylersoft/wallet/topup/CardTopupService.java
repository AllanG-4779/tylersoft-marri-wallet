package net.tylersoft.wallet.topup;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.common.FTRequest;
import net.tylersoft.wallet.common.TransactionStatus;
import net.tylersoft.wallet.gateway.PaymentGatewayPort;
import net.tylersoft.wallet.repository.AccountRepository;
import net.tylersoft.wallet.repository.ChargeConfigRepository;
import net.tylersoft.wallet.repository.ServiceManagementRepository;
import net.tylersoft.wallet.repository.TrxMessageRepository;
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

    private static final String SERVICE_CODE = "CARD_TOPUP";

    private final TransactionSteps steps;
    private final PaymentGatewayPort paymentGateway;
    private final ServiceManagementRepository serviceManagementRepository;
    private final AccountRepository accountRepository;
    private final ChargeConfigRepository chargeConfigRepository;
    private final TrxMessageRepository trxMessageRepository;

    private TransactionPipeline initiatePipeline;

    @PostConstruct
    void init() {
        initiatePipeline = TransactionPipeline.builder()
                .step(steps.staging())
                .step(steps.validateCardTopupTransaction())
                .step(steps.validateCharges())
                .step(steps.validateCardTopupLimits())
                .step(steps.initiateCardCharge(paymentGateway))
                .onFailure(steps.markFailed())
                .build();
    }

    /**
     * Phase 1 — initiates a card topup.
     *
     * <ol>
     *   <li>Resolves the GL suspense account from the CARD_TOPUP service configuration.</li>
     *   <li>Runs the initiate pipeline: staging → validate → charges → limits → PG charge.</li>
     *   <li>Returns immediately with status {@code CALLBACK_WAIT} — the transaction is not
     *       fully posted until the payment gateway callback arrives.</li>
     * </ol>
     */
    public Mono<CardTopupInitiateResponse> initiate(CardTopupRequest request) {
        return serviceManagementRepository.findByServiceCode(SERVICE_CODE)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("CARD_TOPUP service is not configured")))
                .flatMap(svc -> {
                    if (svc.getAccountId() == null)
                        return Mono.error(new IllegalArgumentException("CARD_TOPUP service has no GL account configured"));
                    return accountRepository.findById(svc.getAccountId())
                            .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                    "GL account not found for CARD_TOPUP service")));
                })
                .flatMap(glAccount -> {
                    FTRequest ftReq = FTRequest.builder()
                            .debitAccount(glAccount.getAccountNumber())
                            .creditAccount(request.creditAccount())
                            .amount(request.amount())
                            .currency(request.currency())
                            .transactionType(SERVICE_CODE)
                            .phoneNumber(request.phoneNumber())
                            .build();

                    TransactionContext initialCtx = TransactionContext.builder()
                            .request(ftReq)
                            .totalCharge(BigDecimal.ZERO)
                            .failed(false)
                            .cardDetails(request.card())
                            .build();

                    log.info("Card topup initiate credit={} amount={} currency={}",
                            request.creditAccount(), request.amount(), request.currency());

                    return initiatePipeline.run(initialCtx);
                })
                .map(ctx -> {
                    if (ctx.isSuccessful()) {
                        return new CardTopupInitiateResponse(
                                String.valueOf(ctx.getStagedMessage().getId()),
                                ctx.getStagedMessage().getTransactionRef(),
                                TransactionStatus.CALLBACK_WAIT.name(),
                                "Card charge initiated. Awaiting payment gateway callback."
                        );
                    }
                    return new CardTopupInitiateResponse(
                            null,
                            null,
                            TransactionStatus.FAILED.name(),
                            ctx.getFailureCode() + " - " + ctx.getFailureMessage()
                    );
                });
    }

    /**
     * Phase 2 — handles the payment gateway callback.
     *
     * <p>On a successful callback the transaction is fully posted (DR GL, CR customer wallet).
     * On a failure callback the staged record is marked {@code FAILED}.
     *
     * @param callbackRequest the payload received from the payment gateway
     */
    public Mono<Void> handleCallback(CardTopupCallbackRequest callbackRequest) {
        long esbRef;
        try {
            esbRef = Long.parseLong(callbackRequest.esbRef());
        } catch (NumberFormatException e) {
            return Mono.error(new IllegalArgumentException("Invalid esbRef: " + callbackRequest.esbRef()));
        }

        boolean pgSuccess = "00".equals(callbackRequest.responseCode());

        if (!pgSuccess) {
            log.warn("Card topup callback FAILED esbRef={} code={}", esbRef, callbackRequest.responseCode());
            return steps.asyncUpdateStatus(esbRef, TransactionStatus.FAILED,
                    callbackRequest.responseCode(), callbackRequest.responseMessage()).then();
        }

        log.info("Card topup callback SUCCESS esbRef={} receipt={}", esbRef, callbackRequest.receiptNumber());

        return trxMessageRepository.findById(esbRef)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Transaction not found: " + esbRef)))
                .flatMap(msg -> {
                    // Store receipt number on the staged message before posting
                    msg.setReceiptNumber(callbackRequest.receiptNumber());
                    return trxMessageRepository.save(msg);
                })
                .flatMap(this::reconstructContext)
                .flatMap(ctx -> TransactionPipeline.builder()
                        .step(steps.post())
                        .onFailure(steps.markFailed())
                        .build()
                        .run(ctx))
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
                serviceManagementRepository.findByServiceCode(msg.getTransactionType())
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
