package net.tylersoft.payment.billing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VendRequest(
        @JsonProperty("Amount") String amount,
        @JsonProperty("Currency") String currency,
        @JsonProperty("ClientId") String clientId,
        @JsonProperty("Serviceid") String serviceId,
        @JsonProperty("Phonenumber") String phoneNumber,
        @JsonProperty("accountno") String accountNo,
        @JsonProperty("TransactionId") String transactionId,
        @JsonProperty("email") String email
) {}
