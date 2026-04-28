package net.tylersoft.wallet.topup;

public record CardTopupRequest(
        String creditAccount,
        double amount,
        String currency,
        String phoneNumber,

        /* Same tranid used in the device-fingerprint step — links device profile to this payment. */
        String tranid,
        /* referenceId returned by the device-fingerprint endpoint — required for 3D Secure. */
        String referenceId,
        CardDetails card,
        // Optional — client-supplied overrides; defaults are applied in PaymentServiceGateway
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
