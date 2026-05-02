package net.tylersoft.payment.ott.dto;

import java.math.BigDecimal;

public record OttVoucherResponse(
        String     uniqueReference,
        String     pin,
        String     serialNumber,
        Long       voucherId,
        String     batch,
        String     instructions,
        BigDecimal amount,
        String     rawResponse
) {}
