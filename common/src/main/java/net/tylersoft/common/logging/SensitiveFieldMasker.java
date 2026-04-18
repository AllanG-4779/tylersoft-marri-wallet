package net.tylersoft.common.logging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SensitiveFieldMasker {

    public String mask(String value, MaskingStrategy strategy, int visibleChars) {
        if (value == null || value.isEmpty()) return value;
        return switch (strategy) {
            case FULL         -> "****";
            case PARTIAL_LEFT -> partialLeft(value, visibleChars);
            case PARTIAL_RIGHT -> partialRight(value, visibleChars);
            case EMAIL        -> maskEmail(value);
            case PHONE        -> maskPhone(value, visibleChars);
            case CARD         -> maskCard(value);
            case HASH         -> sha256(value);
        };
    }

    /** ****4321 — keeps the last {@code visible} characters */
    private String partialLeft(String value, int visible) {
        if (value.length() <= visible) return "****";
        int maskLen = value.length() - visible;
        return "*".repeat(maskLen) + value.substring(maskLen);
    }

    /** 1234**** — keeps the first {@code visible} characters */
    private String partialRight(String value, int visible) {
        if (value.length() <= visible) return "****";
        return value.substring(0, visible) + "*".repeat(value.length() - visible);
    }

    /** j***@domain.com */
    private String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0) return "****";
        String local = value.substring(0, at);
        String domain = value.substring(at); // includes @
        if (local.length() == 1) return local + "***" + domain;
        return local.charAt(0) + "*".repeat(local.length() - 1) + domain;
    }

    /** +267****141 — keeps the first 4 chars (dial prefix) and last {@code visible} digits */
    private String maskPhone(String value, int visible) {
        String normalized = value.replaceAll("[\\s\\-()]", "");
        int prefixLen = normalized.startsWith("+") ? 4 : 3;
        if (normalized.length() <= prefixLen + visible) return "****";
        String prefix = normalized.substring(0, prefixLen);
        String suffix = normalized.substring(normalized.length() - visible);
        int maskLen = normalized.length() - prefixLen - visible;
        return prefix + "*".repeat(maskLen) + suffix;
    }

    /** ****-****-****-4321 */
    private String maskCard(String value) {
        String digits = value.replaceAll("[^\\d]", "");
        if (digits.length() < 4) return "****-****-****-****";
        String last4 = digits.substring(digits.length() - 4);
        return "****-****-****-" + last4;
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "****";
        }
    }
}
