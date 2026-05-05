package net.tylersoft.payment.ott.dto;

import jakarta.validation.constraints.NotBlank;

public record OttRemitVoucherRequest(
        String  account,         // optional — OTT user account number
        @NotBlank String amount,
        String  clientId,        // optional — identifying ID
        @NotBlank String mobile,
        @NotBlank String pin,
        @NotBlank String uniqueReference
) {}
