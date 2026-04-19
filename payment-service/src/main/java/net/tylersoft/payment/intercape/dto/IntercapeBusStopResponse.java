package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntercapeBusStopResponse(
        @JsonProperty("Content") Content content,
        @JsonProperty("ErrorCode") String errorCode,
        @JsonProperty("ErrorMessage") String errorMessage,
        @JsonProperty("TransactionId") String transactionId
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(@JsonProperty("Stop") List<Stop> stops) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stop(
            @JsonProperty("Stopname") String stopName,
            @JsonProperty("Latitude") double latitude,
            @JsonProperty("Longitude") double longitude,
            @JsonProperty("Location") String location
    ) {}
}
