package net.tylersoft.payment.ott.dto;

import jakarta.validation.constraints.NotBlank;

public record OttCheckVoucherRequest(
        @NotBlank String voucherPin
) {}
