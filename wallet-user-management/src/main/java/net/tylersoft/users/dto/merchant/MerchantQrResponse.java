package net.tylersoft.users.dto.merchant;

public record MerchantQrResponse(
        String merchantCode,
        String merchantName,
        String businessPhone,
        String accountNumber,
        String qrData,
        String qrImageBase64
) {}
