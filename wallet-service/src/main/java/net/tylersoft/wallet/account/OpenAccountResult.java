package net.tylersoft.wallet.account;

/**
 * Mirrors the three OUT parameters of {@code sp_open_account}:
 * {@code status_code}, {@code message}, {@code accountNo}.
 */
public record OpenAccountResult(String statusCode, String message, String accountNo) {

    public static OpenAccountResult success(String accountNo) {
        return new OpenAccountResult("00", "Account created", accountNo);
    }

    public static OpenAccountResult error(String code, String message) {
        return new OpenAccountResult(code, message, "");
    }

    public boolean isSuccess() {
        return "00".equals(statusCode);
    }
}
