package net.tylersoft.wallet.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IntegratorFTRequest(
        @NotBlank String debitAccount,
        String creditAccount,
        String recipientPhone,
        @NotNull @Positive Double amount,
        @NotBlank String currency,
        String transactionCode,
        String requestRef
) {}
