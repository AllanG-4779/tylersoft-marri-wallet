package net.tylersoft.wallet.ott;

public record OttRedeemResponse(
        String reference,
        String status,
        String message,
        String amount
) {}
