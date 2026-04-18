package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IntercapeBookingTotalRequest(
        @JsonProperty("Header") Header header,
        @JsonProperty("Content") Content content,
        @JsonProperty("Error") String error
) {
    public record Header(
            @JsonProperty("MessageType") String messageType,
            @JsonProperty("Tracenumber") String tracenumber,
            @JsonProperty("Type") String type
    ) {}

    public record Content(
            @JsonProperty("Serviceid") String serviceId,
            @JsonProperty("TransactionId") String transactionId,
            @JsonProperty("BasketID") int basketId,
            @JsonProperty("TripDetails") TripDetails tripDetails
    ) {}

    public record TripDetails(@JsonProperty("Trip") List<TripItem> trips) {}

    public record TripItem(
            @JsonProperty("travelClass") String travelClass,
            @JsonProperty("TripID") String tripId,
            @JsonProperty("NumTix") String numTix,
            @JsonProperty("DepPlace") String depPlace,
            @JsonProperty("ArrPlace") String arrPlace,
            @JsonProperty("CoachSerial") long coachSerial,
            @JsonProperty("Price") int price,
            @JsonProperty("Passenger") List<Passenger> passengers
    ) {}

    public record Passenger(
            @JsonProperty("FirstName") String firstName,
            @JsonProperty("LastName") String lastName,
            @JsonProperty("CellNo") String cellNo,
            @JsonProperty("Contact") String contact,
            @JsonProperty("BabyOnLap") String babyOnLap,
            @JsonProperty("Discount") String discount,
            @JsonProperty("date") String date
    ) {}
}
