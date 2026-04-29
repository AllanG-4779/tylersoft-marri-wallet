package net.tylersoft.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication(scanBasePackages = {"net.tylersoft.payment", "net.tylersoft.common"})
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
    @Bean("smsWebClient")
    public WebClient smsWebClient() {
        return WebClient.builder()
                .baseUrl("https://mobileapigateway.ekenya.co.ke:19171/smsengine")
                .build();
    }

}
