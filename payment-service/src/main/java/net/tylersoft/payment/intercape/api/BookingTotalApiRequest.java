package net.tylersoft.payment.intercape.api;

import java.util.List;

public record BookingTotalApiRequest(
        String transactionId,
        int basketId,
        String tripId,
        int numTickets,
        String depPlace,
        String arrPlace,
        long coachSerial,
        String travelClass,
        int price,
        List<PassengerRequest> passengers
) {}
