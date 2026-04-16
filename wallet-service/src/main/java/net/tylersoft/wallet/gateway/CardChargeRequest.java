package net.tylersoft.wallet.gateway;

public record CardChargeRequest(
        String esbRef,
        String pan,
        String cvv,
        String expiry,
        double amount,
        String currency,
        String phoneNumber
) {}
