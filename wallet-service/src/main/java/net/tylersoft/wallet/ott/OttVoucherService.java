package net.tylersoft.wallet.ott;

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
public class OttVoucherService {

    private final TransactionSteps            steps;
    private final OttVoucherGatewayPort       ottGateway;
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
                .step(steps.initiateOttVoucher(ottGateway))
                .step(steps.post())
                .onFailure(steps.markFailed())
                .build();
    }

    /**
     * Purchases an OTT Botswana e-voucher by debiting the customer's wallet.
     *
     * Balance flow (synchronous):
     * - {@code initiateOttVoucher()} calls the OTT gateway via payment-service.
     *   If it fails the pipeline short-circuits to {@code markFailed()} and balances
     *   are never touched — implicit rollback.
     * - {@code post()} atomically deducts both actualBalance and availableBalance
     *   on the debit account and credits the OTT GL account only on success.
     */
    public Mono<OttVoucherPurchaseResponse> purchase(OttVoucherPurchaseRequest request) {
        return sysServiceRepository.findByTransactionType("OTT_VOUCHER")
                .switchIfEmpty(Mono.error(new IllegalArgumentException("OTT_VOUCHER service not configured")))
                .flatMap(sysService ->
                        serviceManagementRepository.findByServiceIdAndServiceCode(sysService.getId(), "OTT")
                                .switchIfEmpty(Mono.error(new IllegalArgumentException("OTT service configuration not found")))
                )
                .flatMap(svc ->
                        accountRepository.findById(svc.getAccountId())
                                .switchIfEmpty(Mono.error(new IllegalStateException("OTT GL account not found")))
                                .map(glAccount -> FTRequest.builder()
                                        .debitAccount(request.debitAccount())
                                        .creditAccount(glAccount.getAccountNumber())
                                        .amount(request.amount())
                                        .currency(request.currency())
                                        .transactionType("OTT_VOUCHER")
                                        .transactionCode("OTT")
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

                    log.info("OTT voucher purchase debit={} mobile={} amount={}",
                            request.debitAccount(), request.recipientPhone(), request.amount());

                    return pipeline.run(ctx);
                })
                .map(ctx -> {
                    if (ctx.isSuccessful()) {
                        var msg = ctx.getStagedMessage();
                        return new OttVoucherPurchaseResponse(
                                msg.getTransactionRef(),
                                msg.getReceiptNumber(),  // pin stored here by initiateOttVoucher step
                                TransactionStatus.COMPLETED.name(),
                                "OTT voucher purchased successfully"
                        );
                    }
                    return new OttVoucherPurchaseResponse(
                            null, null,
                            TransactionStatus.FAILED.name(),
                            ctx.getFailureCode() + " - " + ctx.getFailureMessage()
                    );
                });
    }
}
