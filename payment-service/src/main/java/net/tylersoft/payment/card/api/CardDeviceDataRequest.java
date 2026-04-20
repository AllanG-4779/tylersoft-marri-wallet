package net.tylersoft.payment.card.api;

import jakarta.validation.constraints.NotBlank;

public record CardDeviceDataRequest(
        @NotBlank String tranid,
        @NotBlank String amount,
        String currency,
        String country,
        @NotBlank String firstname,
        String secondname,
        @NotBlank String phone,
        @NotBlank String email,
        @NotBlank String cardNumber,
        @NotBlank String cardExpiryMonth,
        @NotBlank String cardExpiryYear,
        @NotBlank String cardCvv,
        @NotBlank String cardType
) {}
