package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IntercapeBusStopRequest(
        @JsonProperty("Messagetype") String messagetype,
        @JsonProperty("Tracenumber") String tracenumber
) {}
