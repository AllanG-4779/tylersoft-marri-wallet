package net.tylersoft.users.dto.merchant;

import java.math.BigDecimal;

public record MerchantQrResponse(
        String merchantCode,
        String merchantName,
        String businessPhone,
        String accountNumber,
        String label,
        BigDecimal fixedAmount,
        String qrData,
        String qrImageBase64
) {}
