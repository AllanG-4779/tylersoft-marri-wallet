package net.tylersoft.wallet.topup;

public record CardTopupPaymentRequest(
        String esbRef,
        CardDetails card,
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
