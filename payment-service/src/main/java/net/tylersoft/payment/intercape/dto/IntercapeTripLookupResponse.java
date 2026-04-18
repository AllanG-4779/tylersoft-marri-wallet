package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntercapeTripLookupResponse(
        @JsonProperty("Content") Content content,
        @JsonProperty("TransactionId") String transactionId,
        @JsonProperty("Serviceid") String serviceId
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(@JsonProperty("Trip") List<Trip> trips) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Trip(
            @JsonProperty("ArrWhere") String arrWhere,
            @JsonProperty("ArrPlace") String arrPlace,
            @JsonProperty("DepPlace") String depPlace,
            @JsonProperty("ArrTime") String arrTime,
            @JsonProperty("Price") List<Price> prices,
            @JsonProperty("DepWhere") String depWhere,
            @JsonProperty("Service") String service,
            @JsonProperty("CoachSerial") long coachSerial,
            @JsonProperty("Notes") String notes,
            @JsonProperty("RouteCode") String routeCode,
            @JsonProperty("DepTime") String depTime
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Price(
            @JsonProperty("Amount") int amount,
            @JsonProperty("ClassName") String className,
            @JsonProperty("RetPrice") int retPrice,
            @JsonProperty("Seats") int seats
    ) {}
}
