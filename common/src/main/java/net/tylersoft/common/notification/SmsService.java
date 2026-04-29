package net.tylersoft.common.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class SmsService {

    private final WebClient webClient;

    @Value("${payment.service.base-url}")
    private String paymentServiceBaseUrl;

    public SmsService(WebClient webClient) {
        this.webClient = webClient;
    }

    public void send(String phoneNumber, String message) {
        webClient.get()
                .uri(paymentServiceBaseUrl + "/api/v1/third-party/send-sms/{phoneNumber}/{message}",
                        phoneNumber, message)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.warn("SMS failed to={} reason={}", phoneNumber, e.getMessage()))
                .subscribe();
    }
}
