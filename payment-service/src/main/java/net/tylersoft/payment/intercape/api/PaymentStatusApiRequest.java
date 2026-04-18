package net.tylersoft.payment.intercape.api;

public record PaymentStatusApiRequest(
        String transactionId,
        String ticketSerial,
        String status
) {}
