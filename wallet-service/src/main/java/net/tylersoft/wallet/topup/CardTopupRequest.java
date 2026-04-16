package net.tylersoft.wallet.topup;

public record CardTopupRequest(
        String creditAccount,
        double amount,
        String currency,
        String phoneNumber,
        CardDetails card
) {}
