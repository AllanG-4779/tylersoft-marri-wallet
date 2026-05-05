package net.tylersoft.payment.ott.dto;

import jakarta.validation.constraints.NotBlank;

public record OttCheckRemitVoucherRequest(
        @NotBlank String uniqueReference
) {}
