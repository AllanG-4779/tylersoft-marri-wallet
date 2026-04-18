package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntercapePaymentStatusResponse(
        @JsonProperty("Content") Content content,
        @JsonProperty("TicketSerial") String ticketSerial,
        @JsonProperty("MessageType") String messageType,
        @JsonProperty("ReferenceNo") String referenceNo,
        @JsonProperty("TraceNo") String traceNo,
        @JsonProperty("TransactionId") String transactionId
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(@JsonProperty("PaymentStatus") String paymentStatus) {}
}
