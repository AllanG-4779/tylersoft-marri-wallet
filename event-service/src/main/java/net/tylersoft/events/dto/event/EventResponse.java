package net.tylersoft.events.dto.event;

import net.tylersoft.events.common.EventStatus;
import net.tylersoft.events.common.EventTimeDisplay;
import net.tylersoft.events.common.EventType;
import net.tylersoft.events.common.EventVisibility;
import net.tylersoft.events.model.Event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID organizationId,
        UUID createdBy,
        UUID approvedBy,
        String title,
        String slug,
        UUID categoryId,
        EventType eventType,
        EventVisibility visibility,
        String shortDescription,
        String description,
        EventStatus status,
        OffsetDateTime approvedAt,
        String approvalNotes,
        String rejectionReason,
        String venueName,
        String venueAddress,
        String venueCity,
        String venueCountry,
        BigDecimal venueLatitude,
        BigDecimal venueLongitude,
        String onlineEventUrl,
        LocalDate startDate,
        LocalTime startTime,
        LocalDate endDate,
        LocalTime endTime,
        String timezone,
        EventTimeDisplay timeDisplay,
        Integer totalCapacity,
        String bannerUrl,
        String logoUrl,
        OffsetDateTime salesStartAt,
        OffsetDateTime salesEndAt,
        boolean closeSalesAtCapacity,
        int minTicketsPerOrder,
        Integer maxTicketsPerOrder,
        boolean allowGroupPurchases,
        boolean showRemainingTickets,
        boolean allowMultipleEntries,
        boolean enableCheckinsStaff,
        Integer minAge,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static EventResponse from(Event e) {
        return new EventResponse(
                e.getId(), e.getOrganizationId(), e.getCreatedBy(), e.getApprovedBy(),
                e.getTitle(), e.getSlug(), e.getCategoryId(), e.getEventType(), e.getVisibility(),
                e.getShortDescription(), e.getDescription(), e.getStatus(),
                e.getApprovedAt(), e.getApprovalNotes(), e.getRejectionReason(),
                e.getVenueName(), e.getVenueAddress(), e.getVenueCity(), e.getVenueCountry(),
                e.getVenueLatitude(), e.getVenueLongitude(), e.getOnlineEventUrl(),
                e.getStartDate(), e.getStartTime(), e.getEndDate(), e.getEndTime(),
                e.getTimezone(), e.getTimeDisplay(), e.getTotalCapacity(),
                e.getBannerUrl(), e.getLogoUrl(), e.getSalesStartAt(), e.getSalesEndAt(),
                e.isCloseSalesAtCapacity(), e.getMinTicketsPerOrder(), e.getMaxTicketsPerOrder(),
                e.isAllowGroupPurchases(), e.isShowRemainingTickets(), e.isAllowMultipleEntries(),
                e.isEnableCheckinsStaff(), e.getMinAge(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
