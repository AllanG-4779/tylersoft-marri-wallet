package net.tylersoft.payment;

import lombok.extern.slf4j.Slf4j;
import net.tylersoft.payment.config.SmsProperties;
import net.tylersoft.payment.dto.SendSmsPayload;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@SuppressWarnings("NullableProblems")
@Slf4j
@Component
public class SmsService {

    private final SmsProperties smsProperties;

    private final WebClient webClient;

    @Value("${sms.username}")
    private String smsUsername;

    @Value("${sms.password}")
    private String password;



    public SmsService(@Qualifier("smsWebClient") WebClient webClient, SmsProperties smsProperties) {
        this.webClient = webClient;
        this.smsProperties = smsProperties;
    }

    public String generateSignature(String payload, String key) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC-SHA512 signature", e);
        }
    }

    public Mono<String> sendSms(String phoneNumber, String message) {
        return getSmsAccessToken()
                .flatMap(token -> webClient
                        .mutate()
                        .defaultHeader("Authorization", String.format("Bearer %s", token))
                        .build()
                        .post()
                        .uri("/v2/single")
                        .bodyValue(SendSmsPayload.builder()
                                .to(phoneNumber).message(message).build())
                        .retrieve()
                        .bodyToMono(Map.class)
                        .doOnNext(response -> log.info("Message sent successful {}", response))
                        .flatMap(response -> Mono.just((String) response.get("ResultDesc"))));
    }

    public Mono<String> getSmsAccessToken() {
        return webClient.post()
                .uri("/v3/oauth/token?grant_type=client_credentials")
                .header("Authorization", "Basic " + Base64.getEncoder()
                        .encodeToString(String.format("%s:%s", smsProperties.getKenyanSmsUsername(), smsProperties.getKenyanSmsPassword()).getBytes()))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, err -> err.bodyToMono(String.class)
                        .flatMap(each -> {
                            log.info("Response from data = {}", each);
                            return Mono.error(new RuntimeException("SMS API error: " + each));
                        }))
                .bodyToMono(Map.class)
                .doOnSuccess(each -> log.info("Data from SMS {}", each))
                .map(each -> (String) each.get("access_token"));
    }
}
