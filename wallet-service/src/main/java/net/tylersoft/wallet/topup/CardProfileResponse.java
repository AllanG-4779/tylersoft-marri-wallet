package net.tylersoft.wallet.topup;

public record CardProfileResponse(
        String esbRef,
        String deviceDataCollectionUrl,
        String accessToken,
        String status,
        String message
) {}
