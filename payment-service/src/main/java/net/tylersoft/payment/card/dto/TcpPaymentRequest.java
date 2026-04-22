package net.tylersoft.payment.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TcpPaymentRequest(
        String tranid,
        String country,
        String amount,
        String cybreferenceId,
        String authkey,
        String firstname,
        @JsonProperty("card_number") String cardNumber,
        String org,
        @JsonProperty("card_expiration_year") String cardExpirationYear,
        @JsonProperty("Serviceid") String serviceId,
        String secondname,
        @JsonProperty("card_cvNumber") String cardCvNumber,
        @JsonProperty("card_cardType") String cardCardType,
        String processingcode,
        String phone,
        @JsonProperty("card_expiration_month") String cardExpirationMonth,
        String currency,
        String email,
        String timestamp,
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
