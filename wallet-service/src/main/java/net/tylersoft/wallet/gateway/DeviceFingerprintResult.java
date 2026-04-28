package net.tylersoft.wallet.gateway;

public record DeviceFingerprintResult(
        boolean success,
        String referenceId,
        String statusCode,
        String message,
        String deviceDataCollectionUrl,
        String accessToken
) {}
