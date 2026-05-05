package net.tylersoft.wallet.ott;

import reactor.core.publisher.Mono;

public interface OttRedemptionGatewayPort {
    Mono<OttCheckVoucherGatewayResult> checkVoucher(String voucherPin);
    Mono<OttRemitGatewayResult>        remitVoucher(OttRemitGatewayRequest request);
    Mono<OttRemitGatewayResult>        checkRemitVoucher(String uniqueReference);
}
