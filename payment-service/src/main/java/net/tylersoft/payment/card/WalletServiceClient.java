package net.tylersoft.payment.card;

import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.http.dto.ChannelDetails;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class WalletServiceClient {

    private static final ChannelDetails INTERNAL_CHANNEL =
            new ChannelDetails("payment-service", "payment-service", null, null, "INTEGRATOR", null, null);

    private final WebClient webClient;

    public WalletServiceClient(@Qualifier("walletServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> notifyTopupCallback(String esbRef, String responseCode,
                                          String responseMessage, String receiptNumber) {
        var payload = new UniversalRequestWrapper<>(
                new WalletTopupCallback(esbRef, responseCode, responseMessage, receiptNumber),
                INTERNAL_CHANNEL
        );
        log.info("Forwarding topup callback to wallet-service esbRef={} code={}", esbRef, responseCode);
        return webClient.post()
                .uri("/api/v2/topup/card/callback")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(ex -> {
                    log.error("Failed to notify wallet-service esbRef={}", esbRef, ex);
                    return Mono.empty();
                });
    }
}
