package net.tylersoft.wallet.gateway;

public record CardChargeRequest(
        String esbRef,
        String referenceId,
        String pan,
        String cvv,
        String expiry,
        String cardType,
        double amount,
        String currency,
        String phoneNumber,
        // Optional — null means PaymentServiceGateway applies defaults
        String cardholderName,
        String email,
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
