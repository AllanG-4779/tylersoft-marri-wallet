package net.tylersoft.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SendSMSNotification(
        String recipient,
        String from,
        String to,
        String username,
        String password,
        @JsonProperty("clientId")
        String clientId,
        @JsonProperty("TransactionId")
        String transactionId,
        String message
) {
}
