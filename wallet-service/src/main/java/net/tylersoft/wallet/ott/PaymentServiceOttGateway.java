package net.tylersoft.wallet.ott;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PaymentServiceOttGateway implements OttVoucherGatewayPort {

    private final WebClient webClient;

    public PaymentServiceOttGateway(@Qualifier("paymentServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<OttVoucherGatewayResult> purchase(OttVoucherGatewayRequest request) {
        log.info("OTT voucher → payment-service ref={} mobile={} amount={}",
                request.reference(), request.mobileForSms(), request.amount());

        OttGatewayBody body = new OttGatewayBody(
                request.mobileForSms(),
                String.format("%.2f", request.amount()),
                request.reference()
        );
        return webClient.post()
                .uri("/api/v1/ott/voucher")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<OttGatewayResponse>() {})
                .map(resp -> {
                    boolean success = "00".equals(resp.status());
                    log.info("OTT voucher response ref={} status={}", request.reference(), resp.status());
                    OttGatewayResponseData data = resp.data();
                    return new OttVoucherGatewayResult(
                            success,
                            resp.status(),
                            success ? "OTT voucher dispensed" : (resp.error() != null ? resp.error() : resp.message()),
                            request.reference(),
                            data != null ? data.pin() : null,
                            data != null ? data.serialNumber() : null,
                            data != null ? data.rawResponse() : null
                    );
                })
                .onErrorResume(ex -> {
                    log.error("OTT voucher gateway call failed ref={}", request.reference(), ex);
                    return Mono.just(new OttVoucherGatewayResult(
                            false, "OT99", "OTT gateway error: " + ex.getMessage(),
                            request.reference(), null, null, null));
                });
    }
}
