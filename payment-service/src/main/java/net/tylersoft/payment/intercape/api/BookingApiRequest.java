package net.tylersoft.payment.intercape.api;

public record BookingApiRequest(
        String transactionId,
        String tripId,
        long coachSerial,
        String depPlace,
        String arrPlace,
        String travelClass,
        int numTickets,
        int price
) {}
