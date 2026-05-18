package net.tylersoft.wallet.transfer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.wallet.charge.ChargeValueType;
import net.tylersoft.wallet.common.FTRequest;
import net.tylersoft.wallet.model.ChargeConfig;
import net.tylersoft.wallet.repository.AccountRepository;
import net.tylersoft.wallet.repository.ChargeConfigRepository;
import net.tylersoft.wallet.repository.ServiceManagementRepository;
import net.tylersoft.wallet.repository.SysServiceRepository;
import net.tylersoft.wallet.service.FundTransferService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegratorTransferService {

    private static final String TXN_TYPE = "FT";

    private final SysServiceRepository sysServiceRepository;
    private final ServiceManagementRepository serviceManagementRepository;
    private final ChargeConfigRepository chargeConfigRepository;
    private final AccountRepository accountRepository;
    private final FundTransferService fundTransferService;

    public Mono<FTResponse> transfer(IntegratorFTRequest req, String integratorId) {
        String txnCode = req.transactionCode() != null ? req.transactionCode() : TXN_TYPE;

        return resolveCreditAccount(req)
                .flatMap(creditAccount -> sysServiceRepository.findByTransactionType(TXN_TYPE)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("FT service not configured")))
                        .flatMap(svc -> serviceManagementRepository
                                .findByServiceIdAndServiceCode(svc.getId(), txnCode)
                                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                        "Unknown transaction code: " + txnCode))))
                        .flatMap(svcMgmt -> {
                            BigDecimal amount = BigDecimal.valueOf(req.amount());

                            Mono<BigDecimal> feeMono = chargeConfigRepository
                                    .findApplicable(svcMgmt.getId(), amount)
                                    .collectList()
                                    .map(charges -> charges.stream()
                                            .map(c -> computeCharge(c, amount))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add));

                            Mono<String> debitPhoneMono = accountRepository
                                    .findByAccountNumber(req.debitAccount())
                                    .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                            "Debit account not found: " + req.debitAccount())))
                                    .flatMap(debit -> {
                                        return feeMono.flatMap(fee -> {
                                            BigDecimal totalDebit = amount.add(fee);
                                            if (debit.getAvailableBalance().compareTo(totalDebit) < 0) {
                                                return Mono.error(new IllegalStateException(
                                                        "Insufficient balance. Required: " + totalDebit
                                                                + ", Available: " + debit.getAvailableBalance()));
                                            }
                                            return Mono.just(debit.getPhoneNumber());
                                        });
                                    });

                            return debitPhoneMono.flatMap(debitPhone -> {
                                log.info("Integrator FT initiated integrator={} debit={} credit={} amount={} currency={}",
                                        integratorId, req.debitAccount(), creditAccount, req.amount(), req.currency());

                                FTRequest ftReq = FTRequest.builder()
                                        .debitAccount(req.debitAccount())
                                        .creditAccount(creditAccount)
                                        .amount(req.amount())
                                        .currency(req.currency())
                                        .phoneNumber(debitPhone)
                                        .transactionType(TXN_TYPE)
                                        .transactionCode(txnCode)
                                        .transactionRef(req.requestRef())
                                        .build();

                                return fundTransferService.execute(ftReq)
                                        .map(ctx -> ctx.isSuccessful()
                                                ? new FTResponse(
                                                        ctx.getStagedMessage().getTransactionRef(),
                                                        "00",
                                                        "Transaction successful")
                                                : new FTResponse(
                                                        null,
                                                        ctx.getFailureCode(),
                                                        ctx.getFailureMessage()));
                            });
                        }));
    }

    private Mono<String> resolveCreditAccount(IntegratorFTRequest req) {
        if (req.creditAccount() != null && !req.creditAccount().isBlank()) {
            return Mono.just(req.creditAccount());
        }
        if (req.recipientPhone() != null && !req.recipientPhone().isBlank()) {
            return accountRepository.findTopByPhoneNumber(req.recipientPhone())
                    .map(a -> a.getAccountNumber())
                    .switchIfEmpty(Mono.error(new IllegalArgumentException(
                            "No wallet account found for phone: " + req.recipientPhone())));
        }
        return Mono.error(new IllegalArgumentException(
                "Either creditAccount or recipientPhone must be provided"));
    }

    private BigDecimal computeCharge(ChargeConfig charge, BigDecimal amount) {
        return switch (charge.getValueType()) {
            case FIXED -> charge.getChargeValue();
            case PERCENTAGE -> amount
                    .multiply(charge.getChargeValue())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        };
    }
}
