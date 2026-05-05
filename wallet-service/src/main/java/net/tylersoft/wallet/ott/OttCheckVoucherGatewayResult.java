package net.tylersoft.wallet.ott;

public record OttCheckVoucherGatewayResult(
        boolean success,
        String  serial,
        String  voucherId,
        String  value,
        String  message,
        String  errorCode
) {}
