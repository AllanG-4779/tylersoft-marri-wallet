package net.tylersoft.wallet.topup;

public record CardProfileRequest(
        String creditAccount,
        double amount,
        String currency,
        String phoneNumber,
        String tranid,
        CardDetails card,
        String cardholderName,
        String email
) {}
