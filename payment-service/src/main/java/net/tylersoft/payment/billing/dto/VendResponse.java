package net.tylersoft.payment.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VendResponse(
        @JsonProperty("Message") String message,
        @JsonProperty("Responsecode") String responseCode,
        @JsonProperty("Amount") String amount,
        @JsonProperty("Currency") String currency,
        @JsonProperty("accountno") String accountNo,
        @JsonProperty("Phonenumber") String phoneNumber,
        @JsonProperty("Serviceid") String serviceId,
        @JsonProperty("TransactionId") String transactionId,
        @JsonProperty("description") String description,
        @JsonProperty("ClientId") String clientId,
        // BPC-specific fields
        @JsonProperty("meterNumber") String meterNumber,
        @JsonProperty("tariffIndex") String tariffIndex,
        @JsonProperty("levy") String levy,
        @JsonProperty("businessCharge") String businessCharge,
        @JsonProperty("domesticCharge") String domesticCharge,
        @JsonProperty("receiptNumber") String receiptNumber,
        @JsonProperty("vat") String vat,
        @JsonProperty("vatNumber") String vatNumber,
        @JsonProperty("token") List<TokenItem> tokens
) {}
