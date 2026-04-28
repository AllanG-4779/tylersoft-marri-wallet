package net.tylersoft.wallet.airtime;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.common.FTRequest;
import net.tylersoft.wallet.common.TransactionStatus;
import net.tylersoft.wallet.repository.AccountRepository;
import net.tylersoft.wallet.repository.ServiceManagementRepository;
import net.tylersoft.wallet.repository.SysServiceRepository;
import net.tylersoft.wallet.service.TransactionContext;
import net.tylersoft.wallet.service.TransactionPipeline;
import net.tylersoft.wallet.service.TransactionSteps;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirtimeService {

    private final TransactionSteps            steps;
    private final AirtimeGatewayPort          airtimeGateway;
    private final SysServiceRepository        sysServiceRepository;
    private final ServiceManagementRepository serviceManagementRepository;
    private final AccountRepository           accountRepository;

    private TransactionPipeline pipeline;

    @PostConstruct
    void init() {
        pipeline = TransactionPipeline.builder()
                .step(steps.staging())
                .step(steps.validateTransaction())
                .step(steps.validateCharges())
                .step(steps.validateLimits())
                .step(steps.initiateAirtime(airtimeGateway))
                .step(steps.post())
                .onFailure(steps.markFailed())
                .build();
    }

    /**
     * Purchases airtime for {@code recipientPhone} by debiting the customer's wallet.
     *
     * Balance flow (synchronous — no callback):
     * <ul>
     *   <li>{@code validateLimits()} verifies availableBalance ≥ amount + fees.</li>
     *   <li>{@code initiateAirtime()} calls the airtime gateway. If it fails the pipeline
     *       short-circuits to {@code markFailed()} and balances are never touched.</li>
     *   <li>{@code post()} atomically deducts both actualBalance and availableBalance
     *       on the debit account and credits the GL airtime account.</li>
     * </ul>
     */
    public Mono<AirtimePurchaseResponse> purchase(AirtimePurchaseRequest request) {
        // Resolve sys_service AIRTIME → service config for the requested network → GL credit account
        return sysServiceRepository.findByTransactionType("AIRTIME")
                .switchIfEmpty(Mono.error(new IllegalArgumentException("AIRTIME service not configured")))
                .flatMap(sysService ->
                        serviceManagementRepository.findByServiceIdAndServiceCode(
                                sysService.getId(), request.network())
                                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                        "Network not configured: " + request.network())))
                )
                .flatMap(svc ->
                        accountRepository.findById(svc.getAccountId())
                                .switchIfEmpty(Mono.error(new IllegalStateException(
                                        "GL account not found for network: " + request.network())))
                                .map(glAccount -> FTRequest.builder()
                                        .debitAccount(request.debitAccount())
                                        .creditAccount(glAccount.getAccountNumber())
                                        .amount(request.amount())
                                        .currency(request.currency())
                                        .transactionType("AIRTIME")
                                        .transactionCode(request.network())
                                        .phoneNumber(request.phoneNumber())
                                        .recipientPhoneNumber(request.recipientPhone())
                                        .build())
                )
                .flatMap(ftReq -> {
                    TransactionContext ctx = TransactionContext.builder()
                            .request(ftReq)
                            .totalCharge(BigDecimal.ZERO)
                            .failed(false)
                            .build();

                    log.info("Airtime purchase debit={} network={} recipient={} amount={}",
                            request.debitAccount(), request.network(),
                            request.recipientPhone(), request.amount());

                    return pipeline.run(ctx);
                })
                .map(ctx -> {
                    if (ctx.isSuccessful()) {
                        var msg = ctx.getStagedMessage();
                        return new AirtimePurchaseResponse(
                                msg.getTransactionRef(),
                                msg.getReceiptNumber(),
                                TransactionStatus.COMPLETED.name(),
                                "Airtime purchase successful"
                        );
                    }
                    return new AirtimePurchaseResponse(
                            null, null,
                            TransactionStatus.FAILED.name(),
                            ctx.getFailureCode() + " - " + ctx.getFailureMessage()
                    );
                });
    }
}
