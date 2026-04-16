package net.tylersoft.users.client;

/**
 * Mirrors the {@code ApiResponse<OpenAccountResult>} returned by the wallet-service.
 */
public record WalletAccountResponse(
        boolean success,
        String message,
        AccountResult data
) {
    public record AccountResult(
            String statusCode,
            String message,
            String accountNo
    ) {}
}
