package net.tylersoft.wallet.ott;

public record OttVoucherPurchaseResponse(
        String reference,
        String pin,
        String status,
        String message
) {}
