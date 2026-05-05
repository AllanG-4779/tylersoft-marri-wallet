package net.tylersoft.payment.ott.dto;

public record OttCheckVoucherResponse(
        boolean success,
        String  serial,
        String  voucherId,
        String  value,
        String  message,
        String  errorCode
) {}
