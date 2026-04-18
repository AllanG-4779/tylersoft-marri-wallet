package net.tylersoft.common.logging;

import java.lang.annotation.*;

/**
 * Marks a field as sensitive. When the containing object is serialized via
 * {@link LogSanitizer} the value is masked according to the chosen strategy.
 *
 * <pre>{@code
 * @Sensitive
 * private String password;
 *
 * @Sensitive(strategy = MaskingStrategy.EMAIL)
 * private String email;
 *
 * @Sensitive(strategy = MaskingStrategy.PARTIAL_LEFT, visibleChars = 4)
 * private String cardNumber;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Documented
public @interface Sensitive {
    MaskingStrategy strategy() default MaskingStrategy.FULL;

    /** Number of characters to leave visible for PARTIAL_LEFT / PARTIAL_RIGHT / PHONE strategies. */
    int visibleChars() default 4;
}
