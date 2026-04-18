package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IntercapeBookingPaidRequest(
        @JsonProperty("Header") Header header,
        @JsonProperty("Content") Content content
) {
    public record Header(
            @JsonProperty("MessageType") String messageType,
            @JsonProperty("Tracenumber") String tracenumber,
            @JsonProperty("Type") String type
    ) {}

    public record Content(
            @JsonProperty("BasketID") int basketId,
            @JsonProperty("Serviceid") String serviceId,
            @JsonProperty("TransactionId") String transactionId,
            @JsonProperty("PurchFirstName") String purchFirstName,
            @JsonProperty("PurchLastName") String purchLastName,
            @JsonProperty("PurchContact") String purchContact,
            @JsonProperty("PurchCell") String purchCell,
            @JsonProperty("NextOfKin") String nextOfKin,
            @JsonProperty("TripDetails") TripDetails tripDetails
    ) {}

    public record TripDetails(@JsonProperty("Trip") TripItem trip) {}

    public record TripItem(
            @JsonProperty("travelClass") String travelClass,
            @JsonProperty("DepPlace") String depPlace,
            @JsonProperty("ArrPlace") String arrPlace,
            @JsonProperty("CoachSerial") long coachSerial,
            @JsonProperty("NumTix") int numTix,
            @JsonProperty("TripID") long tripId,
            @JsonProperty("Passenger") List<Passenger> passengers,
            @JsonProperty("Price") int price
    ) {}

    public record Passenger(
            @JsonProperty("Discount") String discount,
            @JsonProperty("FirstName") String firstName,
            @JsonProperty("LastName") String lastName,
            @JsonProperty("CellNo") String cellNo,
            @JsonProperty("Contact") String contact,
            @JsonProperty("BabyOnLap") String babyOnLap
    ) {}
}
