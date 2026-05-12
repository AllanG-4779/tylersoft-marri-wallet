package net.tylersoft.wallet.quote;

public record TransactionEnquiryRequest(
        String transactionType,  // FT, AIRTIME, OTT_VOUCHER
        String transactionCode,  // FT / ORANGE / MASCOM / BTC
        String debitAccount,
        String creditAccount,    // FT: optional — if omitted, recipientPhone is used to resolve the account
        String recipientPhone,   // FT: recipient phone (used when creditAccount is absent); AIRTIME/OTT_VOUCHER: phone to receive
        double amount,
        String currency,
        String phoneNumber       // caller's phone, must match debit account
) {}
