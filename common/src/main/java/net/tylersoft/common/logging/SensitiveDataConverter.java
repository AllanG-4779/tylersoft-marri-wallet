package net.tylersoft.common.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * Logback {@link MessageConverter} registered as {@code %safemsg}.
 *
 * <p>Scans every log line and masks sensitive data regardless of how the object
 * was stringified — covers plain {@code toString()}, Lombok {@code @ToString},
 * JSON snippets, and raw string concatenation.
 *
 * <p>Three masking passes are applied in order:
 * <ol>
 *   <li>JSON key-value pairs — {@code "password":"secret"} → {@code "password":"****"}</li>
 *   <li>Equals-delimited pairs — {@code password=secret} → {@code password=****}</li>
 *   <li>Card-shaped 16-digit numbers — {@code 4111111111111111} → {@code ****-****-****-1111}</li>
 * </ol>
 *
 * <p>Register in {@code logback-spring.xml} and use {@code %safemsg} in patterns:
 * <pre>{@code
 * <conversionRule conversionWord="safemsg"
 *                 converterClass="net.tylersoft.common.logging.SensitiveDataConverter"/>
 * }</pre>
 */
public class SensitiveDataConverter extends MessageConverter {

    private static final String SENSITIVE_KEYS =
            "password|passwd|pin|secret|token|access_token|refresh_token|accesstoken|refreshtoken" +
            "|cvv|cvc|otp|passphrase|privatekey|private_key|clientsecret|client_secret" +
            "|nationalid|national_id|accountnumber|account_number|pinblock|pin_block|authcode|auth_code";

    /** "key" : "value"  or  "key":"value" */
    private static final Pattern JSON_KV = Pattern.compile(
            "\"(" + SENSITIVE_KEYS + ")\"\\s*:\\s*\"([^\"]*?)\"",
            Pattern.CASE_INSENSITIVE
    );

    /** key=value  (stops at comma, space, closing brace/bracket, or end) */
    private static final Pattern PLAIN_KV = Pattern.compile(
            "\\b(" + SENSITIVE_KEYS + ")=([^,\\s}\\]]+)",
            Pattern.CASE_INSENSITIVE
    );

    /** 16-digit card numbers, optionally separated by spaces or dashes */
    private static final Pattern CARD_NUMBER = Pattern.compile(
            "\\b(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})\\b"
    );

    @Override
    public String convert(ILoggingEvent event) {
        String msg = super.convert(event);
        if (msg == null || msg.isEmpty()) return msg;

        msg = JSON_KV.matcher(msg).replaceAll("\"$1\":\"****\"");
        msg = PLAIN_KV.matcher(msg).replaceAll("$1=****");
        msg = CARD_NUMBER.matcher(msg).replaceAll("****-****-****-$4");

        return msg;
    }
}
