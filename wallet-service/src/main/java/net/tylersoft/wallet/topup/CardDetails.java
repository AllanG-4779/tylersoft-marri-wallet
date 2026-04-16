package net.tylersoft.wallet.topup;

public record CardDetails(
        String pan,
        String cvv,
        String expiry
) {}
