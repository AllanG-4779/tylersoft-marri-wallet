package net.tylersoft.payment.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BillerTokenResponse(
        @JsonProperty("Message") String message,
        @JsonProperty("Responsecode") String responseCode,
        @JsonProperty("payload") Payload payload
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payload(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") String expiresIn
    ) {}
}
