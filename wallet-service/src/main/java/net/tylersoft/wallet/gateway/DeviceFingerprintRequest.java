package net.tylersoft.wallet.gateway;

public record DeviceFingerprintRequest(
        String tranid,
        String amount,
        String currency,
        String country,
        String firstname,
        String secondname,
        String phone,
        String email,
        String cardNumber,
        String cardExpiryMonth,
        String cardExpiryYear,
        String cardCvv,
        String cardType
) {}
