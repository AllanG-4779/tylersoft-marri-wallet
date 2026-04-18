package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IntercapeTripLookupRequest(
        @JsonProperty("Messagetype") String messagetype,
        @JsonProperty("Tracenumber") String tracenumber,
        @JsonProperty("ClientId") String clientId,
        @JsonProperty("Serviceid") String serviceId,
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("TransactionId") String transactionId,
        @JsonProperty("DepPlace") String depPlace,
        @JsonProperty("ArrPlace") String arrPlace
) {}
