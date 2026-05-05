package net.tylersoft.payment.ott.dto;

public record OttRemitVoucherResponse(
        boolean success,
        String  voucherId,
        String  voucherAmount,
        String  voucherBalance,
        String  errorCode,
        String  message
) {}
