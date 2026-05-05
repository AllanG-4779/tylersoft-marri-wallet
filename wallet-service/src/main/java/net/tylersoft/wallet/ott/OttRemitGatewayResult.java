package net.tylersoft.wallet.ott;

public record OttRemitGatewayResult(
        boolean success,
        String  voucherId,
        String  voucherAmount,
        String  voucherBalance,
        String  serialNumber,
        String  errorCode,
        String  message
) {}
