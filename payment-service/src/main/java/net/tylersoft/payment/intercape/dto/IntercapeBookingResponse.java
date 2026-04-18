package net.tylersoft.payment.intercape.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntercapeBookingResponse(
        @JsonProperty("Content") Content content,
        @JsonProperty("Serviceid") String serviceId
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            @JsonProperty("Trip") Trip trip,
            @JsonProperty("BasketID") int basketId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Trip(
            @JsonProperty("PriceCheck") boolean priceCheck,
            @JsonProperty("Discounts") Discounts discounts,
            @JsonProperty("TripID") long tripId,
            @JsonProperty("AvailabilityCheck") boolean availabilityCheck
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Discounts(@JsonProperty("Discount") List<Discount> discounts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Discount(
            @JsonProperty("DiscountPercentage") int discountPercentage,
            @JsonProperty("DiscountedPrice") int discountedPrice,
            @JsonProperty("DiscountName") String discountName,
            @JsonProperty("DiscountCode") String discountCode
    ) {}
}
