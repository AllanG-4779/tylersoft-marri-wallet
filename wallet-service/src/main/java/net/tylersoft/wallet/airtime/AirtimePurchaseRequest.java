package net.tylersoft.wallet.airtime;

public record AirtimePurchaseRequest(
        String debitAccount,
        String recipientPhone,
        String network,        // service code e.g. ORANGE, MASCOM, BTC
        double amount,
        String currency,
        String phoneNumber     // caller's phone — must match debit account
) {}
