package net.tylersoft.common.logging;

public enum MaskingStrategy {
    /** Replaces entire value: *** */
    FULL,
    /** Keeps last N chars, masks the rest: ****9141 */
    PARTIAL_LEFT,
    /** Keeps first N chars, masks the rest: +267**** */
    PARTIAL_RIGHT,
    /** j***@domain.com */
    EMAIL,
    /** Keeps dial prefix and last N digits: +267****141 */
    PHONE,
    /** ****-****-****-4321 */
    CARD,
    /** SHA-256 hex digest — preserves equality checking without exposing value */
    HASH
}
