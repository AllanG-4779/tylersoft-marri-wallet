package net.tylersoft.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@EnableConfigurationProperties(SensitiveMaskingProperties.class)
public class LoggingAutoConfiguration {


    @Bean
    public ObjectMapper objectMapper(SensitiveJacksonModule sensitiveJacksonModule) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(sensitiveJacksonModule);
        return mapper;
    }

    /**
     * Field names that are always masked regardless of whether {@link Sensitive}
     * is present. Comparison is case-insensitive.
     */
    private static final Set<String> DEFAULT_SENSITIVE_NAMES = Set.of(
            "password", "pin", "secret", "token", "accesstoken", "refreshtoken",
            "access_token", "refresh_token", "cvv", "cvc", "cardnumber", "card_number",
            "otp", "passphrase", "privatekey", "private_key", "clientsecret",
            "client_secret", "nationalid", "national_id", "accountnumber",
            "account_number", "pinblock", "pin_block", "authcode", "auth_code"
    );

    @Bean
    public SensitiveJacksonModule sensitiveJacksonModule(SensitiveMaskingProperties props) {
        Set<String> names = new HashSet<>(DEFAULT_SENSITIVE_NAMES);
        if (props.getAdditionalFieldNames() != null) {
            props.getAdditionalFieldNames().stream()
                    .map(String::toLowerCase)
                    .forEach(names::add);
        }
        return new SensitiveJacksonModule(Collections.unmodifiableSet(names));
    }

    @Bean
    public LogSanitizer logSanitizer(ObjectMapper objectMapper, SensitiveJacksonModule sensitiveJacksonModule) {
        return new LogSanitizer(objectMapper, sensitiveJacksonModule);
    }
}
