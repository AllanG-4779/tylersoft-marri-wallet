package net.tylersoft.events.dto.event;

import jakarta.validation.constraints.*;
import net.tylersoft.events.common.EventTimeDisplay;
import net.tylersoft.events.common.EventType;
import net.tylersoft.events.common.EventVisibility;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateEventRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 255) String title,
        @NotNull EventType eventType,
        @NotNull LocalDate startDate,
        @NotNull LocalTime startTime,
        @NotNull LocalDate endDate,
        @NotNull LocalTime endTime,
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase alphanumeric with hyphens") String slug,
        UUID categoryId,
        EventVisibility visibility,
        @Size(max = 500) String shortDescription,
        String description,
        String timezone,
        EventTimeDisplay timeDisplay,
        @Positive Integer totalCapacity,
        @Size(max = 255) String venueName,
        @Size(max = 500) String venueAddress,
        @Size(max = 100) String venueCity,
        @Size(max = 100) String venueCountry,
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal venueLatitude,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal venueLongitude,
        @URL String onlineEventUrl,
        String bannerUrl,
        String logoUrl,
        OffsetDateTime salesStartAt,
        OffsetDateTime salesEndAt,
        Boolean closeSalesAtCapacity,
        @Positive Integer minTicketsPerOrder,
        @Positive Integer maxTicketsPerOrder,
        Boolean allowGroupPurchases,
        Boolean showRemainingTickets,
        Boolean allowMultipleEntries,
        Boolean enableCheckinsStaff,
        @PositiveOrZero Integer minAge
) {
    @AssertTrue(message = "End date must not be before start date")
    public boolean isEndDateValid() {
        if (startDate == null || endDate == null) return true;
        return !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "Sales end time must be after sales start time")
    public boolean isSalesEndAtValid() {
        if (salesStartAt == null || salesEndAt == null) return true;
        return salesEndAt.isAfter(salesStartAt);
    }
}
