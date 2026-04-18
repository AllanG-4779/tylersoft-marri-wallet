package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IntercapePaymentStatusRequest(
        @JsonProperty("Messagetype") String messagetype,
        @JsonProperty("Tracenumber") String tracenumber,
        @JsonProperty("TicketSerial") String ticketSerial,
        @JsonProperty("PaymentStatus") String paymentStatus,
        @JsonProperty("ClientId") String clientId,
        @JsonProperty("Serviceid") String serviceId,
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("TransactionId") String transactionId
) {}
