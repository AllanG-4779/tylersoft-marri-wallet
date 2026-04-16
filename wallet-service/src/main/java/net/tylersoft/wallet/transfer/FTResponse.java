package net.tylersoft.wallet.transfer;

public record FTResponse(
        String transactionRef,
        String responseCode,
        String responseMessage
) {}
