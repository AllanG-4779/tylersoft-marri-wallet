package net.tylersoft.payment.intercape;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "intercape")
@Data
public class IntercapeProperties {
    private String baseUrl;
    private String username;
    private String password;
    private String clientId;
    private String serviceId;
}
