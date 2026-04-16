package net.tylersoft.wallet.gateway;

import reactor.core.publisher.Mono;

public interface PaymentGatewayPort {

    /**
     * Initiates a card charge request with the payment gateway.
     *
     * @param request card and transaction details
     * @return the gateway's response — never throws; failures are encoded in {@link CardChargeResult#success()}
     */
    Mono<CardChargeResult> charge(CardChargeRequest request);
}
