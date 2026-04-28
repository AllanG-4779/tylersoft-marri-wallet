package net.tylersoft.wallet.airtime;

import com.fasterxml.jackson.annotation.JsonProperty;

record AirtimeVendRequest(
        String serviceCode,
        String accountNo,
        String amount,
        String currency,
        String phoneNumber,
        String transactionId,
        @JsonProperty("email") String email
) {}
