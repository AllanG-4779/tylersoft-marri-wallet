package net.tylersoft.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "billing")
@Data
public class BillingProperties {

    private String username;
    private String password;
    private String baseUrl;
    private String clientId;
    private String accessTokenEndpoint;
    private String billPaymentEndpoint;
    private List<BillPaymentProperties> services;
    private String defaultBaseUrl;


    public record BillPaymentProperties(String serviceCode, String serviceId, String endpoint) {}

    public BillPaymentProperties findService(String serviceCode) {
        return services.stream()
                .filter(s -> serviceCode.equals(s.serviceCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown billing service: " + serviceCode));
    }

    public String resolveEndpoint(BillPaymentProperties service) {
        return service.endpoint() != null ? service.endpoint() : defaultBaseUrl;
    }
}
