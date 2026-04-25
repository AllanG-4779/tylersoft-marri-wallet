package net.tylersoft.wallet.airtime;

public record AirtimePurchaseResponse(
        String reference,
        String providerReference,
        String status,
        String message
) {}
