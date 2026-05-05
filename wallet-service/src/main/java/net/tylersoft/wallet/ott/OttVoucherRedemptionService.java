package net.tylersoft.wallet.ott;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.common.FTRequest;
import net.tylersoft.wallet.service.TransactionContext;
import net.tylersoft.wallet.service.TransactionPipeline;
import net.tylersoft.wallet.service.TransactionSteps;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Redeems an OTT voucher PIN and credits the value to the customer's wallet.
 *
 * Flow:
 * 1. CheckVoucher — validate the PIN with OTT before touching any wallet state.
 * 2. {@code staging()} — write PENDING record.
 * 3. {@code validateCardTopupTransaction()} — resolve GL debit account from
 *    OTT_REDEMPTION/OTT_REDEEM service management; validate credit (wallet) account.
 * 4. {@code initiateOttRedemption()} — call OTT RemitVoucher; update staged amount
 *    with the actual {@code voucherAmount} returned by OTT.
 * 5. {@code post()} — atomically debit OTT GL, credit wallet, mark COMPLETED.
 *
 * Requires admin configuration: {@code OTT_REDEMPTION} in {@code sys_services} and
 * a matching entry in {@code cfg_service_management} pointing to the OTT GL account
 * (must have {@code allow_dr = TRUE}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OttVoucherRedemptionService {

    private final TransactionSteps          steps;
    private final OttRedemptionGatewayPort  ottRedemptionGateway;

    public Mono<OttRedeemResponse> redeem(String phoneNumber, OttRedeemRequest request) {
        // Step 1: Check the voucher is valid before staging any transaction.
        return ottRedemptionGateway.checkVoucher(request.voucherPin())
                .flatMap(check -> {
                    if (!check.success()) {
                        log.warn("OTT CheckVoucher failed pin=**** code={} msg={}", check.errorCode(), check.message());
                        return Mono.just(new OttRedeemResponse(
                                null, "FAILED",
                                "Voucher invalid: " + check.message(),
                                null));
                    }
                    log.info("OTT CheckVoucher OK pin=**** serial={} value={}", check.serial(), check.value());
                    return runRedemptionPipeline(phoneNumber, request);
                });
    }

    private Mono<OttRedeemResponse> runRedemptionPipeline(String phoneNumber, OttRedeemRequest request) {
        double amount = parseAmount(request.amount());

        FTRequest ftReq = FTRequest.builder()
                .debitAccount("")               // resolved from service management by validateCardTopupTransaction()
                .creditAccount(request.creditAccount())
                .amount(amount)
                .currency(request.currency())
                .transactionType("OTT_REDEMPTION")
                .transactionCode("OTT_REDEEM")
                .phoneNumber(phoneNumber)
                .build();

        TransactionContext ctx = TransactionContext.builder()
                .request(ftReq)
                .totalCharge(BigDecimal.ZERO)
                .failed(false)
                .build();

        log.info("OTT voucher redemption credit={} phone={} amount={}", request.creditAccount(), phoneNumber, amount);

        // Pipeline is built per-request so voucherPin can be captured in the redemption step closure.
        TransactionPipeline pipeline = TransactionPipeline.builder()
                .step(steps.staging())
                .step(steps.validateCardTopupTransaction())
                .step(steps.initiateOttRedemption(ottRedemptionGateway, request.voucherPin()))
                .step(steps.post())
                .onFailure(steps.markFailed())
                .build();

        return pipeline.run(ctx)
                .map(resultCtx -> {
                    if (resultCtx.isSuccessful()) {
                        var msg = resultCtx.getStagedMessage();
                        return new OttRedeemResponse(
                                msg.getTransactionRef(),
                                "COMPLETED",
                                "OTT voucher redeemed successfully",
                                msg.getAmount() != null ? msg.getAmount().toPlainString() : "0");
                    }
                    return new OttRedeemResponse(
                            null,
                            "FAILED",
                            resultCtx.getFailureCode() + " - " + resultCtx.getFailureMessage(),
                            null);
                });
    }

    private static double parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return 0.0;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
