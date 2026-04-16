package net.tylersoft.wallet.common;

public enum TransactionStatus {

    STARTED((short) 0),
    COMPLETED((short) 1),
    FAILED((short) 2),
    CALLBACK_WAIT((short) 3);

    private final short code;

    TransactionStatus(short code) {
        this.code = code;
    }

    public short code() {
        return code;
    }

    public static TransactionStatus fromCode(short code) {
        for (TransactionStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown transaction status code: " + code);
    }
}
