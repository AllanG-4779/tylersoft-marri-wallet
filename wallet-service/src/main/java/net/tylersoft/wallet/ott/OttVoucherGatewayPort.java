package net.tylersoft.wallet.ott;

import reactor.core.publisher.Mono;

public interface OttVoucherGatewayPort {
    Mono<OttVoucherGatewayResult> purchase(OttVoucherGatewayRequest request);
}
