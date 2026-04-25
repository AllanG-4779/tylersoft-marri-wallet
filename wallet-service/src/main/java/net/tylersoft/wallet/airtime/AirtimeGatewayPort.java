package net.tylersoft.wallet.airtime;

import reactor.core.publisher.Mono;

public interface AirtimeGatewayPort {
    Mono<AirtimeGatewayResult> purchase(AirtimeGatewayRequest request);
}
