package net.tylersoft.payment.card;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tcp")
@Data
public class TcpProperties {
    private String baseUrl;
    private String org;
    private String key;
    private String serviceId;
    private String processingCode = "000010";
    private String country = "Botswana";
    private String currency = "BWP";
}
