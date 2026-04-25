package net.tylersoft.wallet.airtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record AirtimeVendResponse(
        @JsonProperty("Message")       String message,
        @JsonProperty("Responsecode")  String responseCode,
        @JsonProperty("TransactionId") String transactionId,
        @JsonProperty("accountno")     String accountNo,
        @JsonProperty("Phonenumber")   String phoneNumber
) {}
