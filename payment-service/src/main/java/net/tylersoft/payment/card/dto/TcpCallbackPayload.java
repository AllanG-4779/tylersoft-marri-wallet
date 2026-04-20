package net.tylersoft.payment.card.dto;

public record TcpCallbackPayload(
        String statuscode,
        String tranid,
        String status
) {}
