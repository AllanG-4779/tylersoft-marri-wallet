package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntercapeBookingTotalResponse(
        @JsonProperty("Content") Content content,
        @JsonProperty("TransactionId") String transactionId,
        @JsonProperty("Serviceid") String serviceId
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            @JsonProperty("AmountDue") int amountDue,
            @JsonProperty("PayBy") long payBy
    ) {}
}
