package net.tylersoft.payment.card;

record WalletTopupCallback(
        String esbRef,
        String responseCode,
        String responseMessage,
        String receiptNumber
) {}
