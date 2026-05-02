package net.tylersoft.wallet.ott;

public record OttVoucherPurchaseRequest(
        String debitAccount,   // user's wallet account number
        String recipientPhone, // phone to receive the voucher SMS (mobileForSms)
        double amount,         // voucher value
        String currency,
        String phoneNumber     // caller's phone — must match debit account
) {}
