package net.tylersoft.payment.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BpcMeterResponse(
        @JsonProperty("canvend") String canVend,
        @JsonProperty("meternumber") String meterNumber,
        @JsonProperty("maxvendamount") String maxVendAmount,
        @JsonProperty("minvendamount") String minVendAmount,
        @JsonProperty("kyc") Kyc kyc,
        @JsonProperty("status") String status,
        @JsonProperty("fault") String fault,
        @JsonProperty("message") String message
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Kyc(
            @JsonProperty("name") String name,
            @JsonProperty("address") String address,
            @JsonProperty("contact") String contact,
            @JsonProperty("utilitytype") String utilityType,
            @JsonProperty("dayslastpurchase") String daysLastPurchase
    ) {}
}
