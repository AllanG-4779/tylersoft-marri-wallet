package net.tylersoft.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sms")
@Data
public class SmsProperties {
    private String apiUrl;
    private String password;
    private String username;
    private String kenyanSmsUsername;
    private String kenyanSmsPassword;
    private String clientId;
    private String sender;
}
