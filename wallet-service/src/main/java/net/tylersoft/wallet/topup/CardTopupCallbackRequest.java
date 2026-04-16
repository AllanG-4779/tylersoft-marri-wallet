package net.tylersoft.wallet.topup;

public record CardTopupCallbackRequest(
        String esbRef,
        String responseCode,
        String responseMessage,
        String receiptNumber
) {}
