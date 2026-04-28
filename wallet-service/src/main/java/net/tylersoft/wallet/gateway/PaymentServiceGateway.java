package net.tylersoft.wallet.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PaymentServiceGateway implements PaymentGatewayPort {

    private static final String COUNTRY = "Botswana";
    private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String LANG = "en-US";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    private final WebClient webClient;

    public PaymentServiceGateway(@Qualifier("paymentServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<DeviceFingerprintResult> deviceFingerprint(DeviceFingerprintRequest req) {
        log.info("Device fingerprint → payment-service tranid={}", req.tranid());
        return webClient.post()
                .uri("/api/v1/card/device-fingerprint")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DeviceFingerprintResponse>() {
                })
                .map(body -> {
                    ;
                    boolean success = "00".equals(body.statuscode());
                    log.info("Device fingerprint response tranid={} statuscode={}", req.tranid(),
                            body.statuscode());
                    return new DeviceFingerprintResult(
                            success,
                            body.referenceId(),
                            body.statuscode(),
                            body.statusmessage(),
                            body.deviceDataCollectionUrl(),
                            body.accessToken()
                    );
                })
                .onErrorResume(ex -> {
                    log.error("Device fingerprint call failed tranid={}", req.tranid(), ex);
                    return Mono.just(new DeviceFingerprintResult(
                            false, null, "PG99", "Payment gateway error: " + ex.getMessage(), null, null));
                });
    }

    @Override
    public Mono<CardChargeResult> charge(CardChargeRequest req) {
        PaymentGatewayRequest body = buildChargeRequest(req);
        log.info("Card charge → payment-service tranid={} referenceId={}", req.esbRef(), req.referenceId());
        return webClient.post()
                .uri("/api/v1/card/payment")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PaymentGatewayResponse>() {
                })
                .map(tcp -> {
                    boolean success = "001".equals(tcp.statuscode());
                    String code = tcp.statuscode();
                    String msg = tcp.message();
                    log.info("Card charge response tranid={} statuscode={}", req.esbRef(), code);
                    return new CardChargeResult(success, code, msg, req.esbRef());
                })
                .onErrorResume(ex -> {
                    log.error("Card charge call failed tranid={}", req.esbRef(), ex);
                    return Mono.just(new CardChargeResult(
                            false, "PG99", "Payment gateway error: " + ex.getMessage(), null));
                });
    }

    private PaymentGatewayRequest buildChargeRequest(CardChargeRequest req) {
        String[] parts = req.expiry().split("/");
        String month = parts[0].trim();
        String year = parts.length > 1 ? parts[1].trim() : "";
        if (year.length() == 2) year = "20" + year;

        String[] names = orDefault(req.cardholderName(), "John Doe").split(" ", 2);
        String firstname  = names[0];
        String secondname = names.length > 1 ? names[1] : "";

        // Payment service expects local phone format — strip Botswana country code if present
        String localPhone = req.phoneNumber() != null && req.phoneNumber().startsWith("267")
                ? req.phoneNumber().substring(3)
                : req.phoneNumber();

        // Amount must always be formatted to 2 decimal places
        String amount = String.format("%.2f", req.amount());

        return new PaymentGatewayRequest(
                req.esbRef(),
                amount,
                req.currency(),
                COUNTRY,
                req.referenceId(),
                firstname,
                secondname,
                localPhone,
                orDefault(req.email(), localPhone + "@wallet.local"),
                req.pan(),
                month,
                year,
                req.cvv(),
                req.cardType(),
                orDefault(req.ipAddress(), "127.0.0.1"),
                orDefault(req.httpAcceptContent(), ACCEPT),
                orDefault(req.httpBrowserLanguage(), LANG),
                orDefault(req.httpBrowserJavaEnabled(), "false"),
                orDefault(req.httpBrowserJavaScriptEnabled(), "true"),
                orDefault(req.httpBrowserColorDepth(), "24"),
                orDefault(req.httpBrowserScreenHeight(), "900"),
                orDefault(req.httpBrowserScreenWidth(), "1440"),
                orDefault(req.httpBrowserTimeDifference(), "0"),
                orDefault(req.userAgentBrowserValue(), UA)
        );
    }

    private String orDefault(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
