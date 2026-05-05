package net.tylersoft.wallet.ott;

public record OttRemitGatewayRequest(
        String account,         // optional
        String amount,
        String clientId,        // optional
        String mobile,
        String pin,
        String uniqueReference
) {}
