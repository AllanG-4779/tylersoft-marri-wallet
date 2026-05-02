package net.tylersoft.users.dto.merchant;

import java.math.BigDecimal;

public record MerchantQrRequest(
        String label,
        BigDecimal fixedAmount
) {}
