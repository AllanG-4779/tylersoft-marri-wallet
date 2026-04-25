package net.tylersoft.payment.card.dto;

public record TcpPaymentResponse(
        String pareq,
        String pastepup,
        String accessToken,
        String message,
        String statuscode
) {}
