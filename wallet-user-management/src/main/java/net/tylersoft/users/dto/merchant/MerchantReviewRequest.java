package net.tylersoft.users.dto.merchant;

import jakarta.validation.constraints.NotNull;

public record MerchantReviewRequest(
        @NotNull String action,   // APPROVE | REJECT
        String reason
) {}