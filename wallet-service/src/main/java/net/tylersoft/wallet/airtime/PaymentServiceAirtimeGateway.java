package net.tylersoft.wallet.airtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PaymentServiceAirtimeGateway implements AirtimeGatewayPort {

    private final WebClient webClient;

    public PaymentServiceAirtimeGateway(@Qualifier("paymentServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<AirtimeGatewayResult> purchase(AirtimeGatewayRequest request) {
        log.info("Airtime vend → payment-service ref={} network={} recipient={}",
                request.reference(), request.network(), request.recipientPhone());

        AirtimeVendRequest body = new AirtimeVendRequest(
              String.format("AIRTIME_%S",request.network()),
                request.recipientPhone(),
                String.format("%.2f", request.amount()),
                request.currency(),
                request.recipientPhone(),
                request.reference(),
                null
        );

        return webClient.post()
                .uri("/api/v1/billing/vend")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<AirtimeVendResponse>() {})
                .map(resp -> {
                    boolean success = "00".equals(resp.responseCode());
                    log.info("Airtime vend response ref={} code={}", request.reference(), resp.responseCode());
                    return new AirtimeGatewayResult(
                            success,
                            resp.responseCode(),
                            resp.message(),
                            resp.transactionId()
                    );
                })
                .onErrorResume(ex -> {
                    log.error("Airtime vend call failed ref={}", request.reference(), ex);
                    return Mono.just(new AirtimeGatewayResult(
                            false, "AT99", "Airtime gateway error: " + ex.getMessage(), null));
                });
    }
}
