package net.tylersoft.payment.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenItem(
        @JsonProperty("units") String units,
        @JsonProperty("desc") String description,
        @JsonProperty("token") String token
) {}
