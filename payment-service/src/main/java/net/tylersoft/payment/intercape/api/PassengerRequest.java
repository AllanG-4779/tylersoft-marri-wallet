package net.tylersoft.payment.intercape.api;

public record PassengerRequest(
        String firstName,
        String lastName,
        String cellNo,
        String discount,
        String date
) {}
