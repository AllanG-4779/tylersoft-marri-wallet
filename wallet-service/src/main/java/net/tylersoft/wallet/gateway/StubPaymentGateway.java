package net.tylersoft.wallet.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@Profile("stub")
public class StubPaymentGateway implements PaymentGatewayPort {

    @Override
    public Mono<DeviceFingerprintResult> deviceFingerprint(DeviceFingerprintRequest request) {
        log.info("StubPaymentGateway: device fingerprint tranid={}", request.tranid());
        return Mono.just(new DeviceFingerprintResult(
                true,
                "REF-" + UUID.randomUUID(),
                "00",
                "Device profiling successful",
                "https://stub.device-collection.url",
                "stub-access-token"
        ));
    }

    @Override
    public Mono<CardChargeResult> charge(CardChargeRequest request) {
        log.info("StubPaymentGateway: charge tranid={} amount={}", request.esbRef(), request.amount());
        return Mono.just(new CardChargeResult(
                true,
                "00",
                "Approved",
                request.esbRef()
        ));
    }
}
