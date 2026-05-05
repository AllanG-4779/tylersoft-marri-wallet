package net.tylersoft.payment.ott.dto;

public record OttCheckRemitVoucherResponse(
        boolean success,
        String  voucherId,
        String  voucherAmount,
        String  voucherBalance,
        String  serialNumber,
        String  errorCode,
        String  message
) {}
