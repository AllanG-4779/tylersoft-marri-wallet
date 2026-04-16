package net.tylersoft.wallet.gateway;

public record CardChargeResult(
        boolean success,
        String responseCode,
        String responseMessage,
        String pgTransactionId
) {}
