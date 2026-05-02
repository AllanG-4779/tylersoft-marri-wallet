package net.tylersoft.wallet.ott;

public record OttVoucherGatewayResult(
        boolean success,
        String  responseCode,
        String  responseMessage,
        String  reference,
        String  pin,
        String  serialNumber,
        String  rawResponse
) {}
