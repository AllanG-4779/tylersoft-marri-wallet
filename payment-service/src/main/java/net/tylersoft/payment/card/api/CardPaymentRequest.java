package net.tylersoft.payment.card.api;

import jakarta.validation.constraints.NotBlank;

public record CardPaymentRequest(
        @NotBlank String tranid,
        @NotBlank String amount,
        String currency,
        String country,
        @NotBlank String referenceId,
        @NotBlank String firstname,
        String secondname,
        @NotBlank String phone,
        @NotBlank String email,
        @NotBlank String cardNumber,
        @NotBlank String cardExpiryMonth,
        @NotBlank String cardExpiryYear,
        @NotBlank String cardCvv,
        @NotBlank String cardType,
        String ipAddress,
        String httpAcceptContent,
        String httpBrowserLanguage,
        String httpBrowserJavaEnabled,
        String httpBrowserJavaScriptEnabled,
        String httpBrowserColorDepth,
        String httpBrowserScreenHeight,
        String httpBrowserScreenWidth,
        String httpBrowserTimeDifference,
        String userAgentBrowserValue
) {}
