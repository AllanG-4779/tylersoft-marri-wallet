package net.tylersoft.payment.billing.api;

public record VendApiRequest(
        String serviceCode,
        String accountNo,
        String amount,
        String currency,
        String phoneNumber,
        String transactionId,
        String email
) {}
