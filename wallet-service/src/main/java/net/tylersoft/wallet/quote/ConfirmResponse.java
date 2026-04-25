package net.tylersoft.wallet.quote;

public record ConfirmResponse(
        String transactionRef,
        String receiptNumber,
        String status,
        String message
) {}
