package net.tylersoft.wallet.quote;

public record TransactionEnquiryRequest(
        String transactionType,  // FT, AIRTIME
        String transactionCode,  // FT / ORANGE / MASCOM / BTC
        String debitAccount,
        String creditAccount,    // FT only — recipient wallet account number
        String recipientPhone,   // AIRTIME only — phone to receive airtime
        double amount,
        String currency,
        String phoneNumber       // caller's phone, must match debit account
) {}
