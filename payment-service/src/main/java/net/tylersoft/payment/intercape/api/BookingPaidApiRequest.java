package net.tylersoft.payment.intercape.api;

import java.util.List;

public record BookingPaidApiRequest(
        String transactionId,
        int basketId,
        String purchFirstName,
        String purchLastName,
        String purchContact,
        String purchCell,
        long tripId,
        long coachSerial,
        String depPlace,
        String arrPlace,
        String travelClass,
        int numTickets,
        int price,
        List<PassengerRequest> passengers
) {}
