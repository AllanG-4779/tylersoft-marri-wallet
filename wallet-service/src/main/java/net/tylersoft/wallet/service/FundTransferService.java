package net.tylersoft.wallet.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.common.FTRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundTransferService {

    private final TransactionSteps steps;

    private TransactionPipeline.Builder transactionReceiptPipeline;

    @PostConstruct
    void init() {
        transactionReceiptPipeline = TransactionPipeline.builder()
                .step(steps.staging())
                .step(steps.validateTransaction())
                .step(steps.validateCharges())
                .step(steps.validateLimits());

    }

    public Mono<TransactionContext> execute(FTRequest request) {
        log.info("FT initiated debit={} credit={} amount={} currency={}",
                request.getDebitAccount(), request.getCreditAccount(),
                request.getAmount(), request.getCurrency());
        var ftPipeline = transactionReceiptPipeline.step(steps.post())
                .onFailure(steps.markFailed())
                .build();

        return ftPipeline.run(request);
    }





}
