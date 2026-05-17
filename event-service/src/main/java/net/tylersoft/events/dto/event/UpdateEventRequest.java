package net.tylersoft.events.dto.event;

import jakarta.validation.constraints.NotNull;
import net.tylersoft.events.common.EventTimeDisplay;
import net.tylersoft.events.common.EventType;
import net.tylersoft.events.common.EventVisibility;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateEventRequest(
        @NotNull UUID id,
        String title,
        String slug,
        UUID categoryId,
        EventType eventType,
        EventVisibility visibility,
        String shortDescription,
        String description,
        String timezone,
        EventTimeDisplay timeDisplay,
        Integer totalCapacity,
        String venueName,
        String venueAddress,
        String venueCity,
        String venueCountry,
        BigDecimal venueLatitude,
        BigDecimal venueLongitude,
        String onlineEventUrl,
        String bannerUrl,
        String logoUrl,
        OffsetDateTime salesStartAt,
        OffsetDateTime salesEndAt,
        Boolean closeSalesAtCapacity,
        Integer minTicketsPerOrder,
        Integer maxTicketsPerOrder,
        Boolean allowGroupPurchases,
        Boolean showRemainingTickets,
        Boolean allowMultipleEntries,
        Boolean enableCheckinsStaff,
        Integer minAge,
        LocalDate startDate,
        LocalTime startTime,
        LocalDate endDate,
        LocalTime endTime
) {}
