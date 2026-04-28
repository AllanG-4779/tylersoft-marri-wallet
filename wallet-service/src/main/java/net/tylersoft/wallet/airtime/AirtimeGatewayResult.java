package net.tylersoft.wallet.airtime;

public record AirtimeGatewayResult(
        boolean success,
        String  responseCode,
        String  responseMessage,
        String  providerReference
) {}
