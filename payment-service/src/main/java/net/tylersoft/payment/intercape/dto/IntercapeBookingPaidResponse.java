package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntercapeBookingPaidResponse(
        @JsonProperty("Content") Content content,
        @JsonProperty("TransactionId") String transactionId,
        @JsonProperty("Serviceid") String serviceId
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            @JsonProperty("TripDetails") TripDetails tripDetails,
            @JsonProperty("TicketSerial") String ticketSerial,
            @JsonProperty("PayBy") long payBy
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TripDetails(@JsonProperty("Trip") Trip trip) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Trip(
            @JsonProperty("NumTix") int numTix,
            @JsonProperty("ArrWhere") String arrWhere,
            @JsonProperty("TicketFooter") String ticketFooter,
            @JsonProperty("ArrPlace") String arrPlace,
            @JsonProperty("TicketHeader") String ticketHeader,
            @JsonProperty("DepPlace") String depPlace,
            @JsonProperty("ArrTime") String arrTime,
            @JsonProperty("DepWhere") String depWhere,
            @JsonProperty("TicketTitle") String ticketTitle,
            @JsonProperty("Passenger") Passenger passenger,
            @JsonProperty("TripID") long tripId,
            @JsonProperty("DepTime") String depTime
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Passenger(
            @JsonProperty("Price") int price,
            @JsonProperty("TicketNo") String ticketNo,
            @JsonProperty("PaxName") String paxName,
            @JsonProperty("Notes") String notes
    ) {}
}
