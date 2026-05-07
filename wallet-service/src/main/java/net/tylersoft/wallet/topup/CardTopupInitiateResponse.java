package net.tylersoft.wallet.topup;

public record CardTopupInitiateResponse(
        String esbRef,
        String transactionRef,
        String status,
        String message,
        String accessToken,
        String redirectUrl,
        boolean authorizationRequired
) {}
