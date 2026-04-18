package net.tylersoft.payment.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BpcMeterRequest(
        @JsonProperty("meternumber") String meterNumber,
        @JsonProperty("ClientId") String clientId
) {}
