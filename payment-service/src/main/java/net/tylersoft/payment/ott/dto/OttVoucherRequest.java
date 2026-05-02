package net.tylersoft.payment.ott.dto;

import jakarta.validation.constraints.NotBlank;

public record OttVoucherRequest(
        @NotBlank String phoneNumber,
        @NotBlank String amount,
        String uniqueReference
) {}
