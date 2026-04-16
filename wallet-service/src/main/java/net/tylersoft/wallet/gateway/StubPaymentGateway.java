package net.tylersoft.wallet.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Stub implementation of {@link PaymentGatewayPort}.
 * Always returns a successful charge result. Replace with a real HTTP integration.
 */
@Slf4j
@Service
public class StubPaymentGateway implements PaymentGatewayPort {

    @Override
    public Mono<CardChargeResult> charge(CardChargeRequest request) {
        log.info("StubPaymentGateway: charging esbRef={} amount={} currency={}",
                request.esbRef(), request.amount(), request.currency());
        return Mono.just(new CardChargeResult(
                true,
                "00",
                "Approved",
                "PG-" + UUID.randomUUID()
        ));
    }
}
