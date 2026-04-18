package net.tylersoft.common.logging;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * Extend the built-in sensitive field name list via application.yml:
 *
 * <pre>
 * tylersoft:
 *   logging:
 *     masking:
 *       additional-field-names:
 *         - nationalId
 *         - referenceToken
 * </pre>
 */
@ConfigurationProperties(prefix = "tylersoft.logging.masking")
@Data
public class SensitiveMaskingProperties {
    private Set<String> additionalFieldNames = new HashSet<>();
}
