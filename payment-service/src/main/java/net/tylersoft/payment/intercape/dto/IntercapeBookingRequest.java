package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IntercapeBookingRequest(
        @JsonProperty("Header") Header header,
        @JsonProperty("Content") Content content,
        @JsonProperty("Error") String error
) {
    public record Header(
            @JsonProperty("Type") String type,
            @JsonProperty("MessageType") String messageType,
            @JsonProperty("Tracenumber") String tracenumber
    ) {}

    public record Content(
            @JsonProperty("ClientId") String clientId,
            @JsonProperty("Serviceid") String serviceId,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("TransactionId") String transactionId,
            @JsonProperty("Trip") List<TripItem> trips
    ) {}

    public record TripItem(
            @JsonProperty("travelClass") String travelClass,
            @JsonProperty("DepPlace") String depPlace,
            @JsonProperty("ArrPlace") String arrPlace,
            @JsonProperty("TripID") String tripId,
            @JsonProperty("CoachSerial") long coachSerial,
            @JsonProperty("NumTix") String numTix,
            @JsonProperty("Price") int price
    ) {}
}
