package net.tylersoft.wallet.gateway;

import reactor.core.publisher.Mono;

public interface PaymentGatewayPort {

    Mono<DeviceFingerprintResult> deviceFingerprint(DeviceFingerprintRequest request);

    Mono<CardChargeResult> charge(CardChargeRequest request);
}
