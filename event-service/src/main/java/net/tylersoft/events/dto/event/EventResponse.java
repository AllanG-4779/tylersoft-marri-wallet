package net.tylersoft.events.dto.event;

import net.tylersoft.events.model.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String merchantCode,
        String title,
        String description,
        String venueName,
        String venueAddress,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String coverImageUrl,
        String status,
        String statusReason,
        OffsetDateTime statusChangedAt,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static EventResponse from(Event e) {
        return new EventResponse(
                e.getId(),
                e.getMerchantCode(),
                e.getTitle(),
                e.getDescription(),
                e.getVenueName(),
                e.getVenueAddress(),
                e.getStartAt(),
                e.getEndAt(),
                e.getCoverImageUrl(),
                e.getStatus(),
                e.getStatusReason(),
                e.getStatusChangedAt(),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
