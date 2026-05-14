package net.tylersoft.events.dto.event;

import java.time.OffsetDateTime;

public record UpdateEventRequest(
        String title,
        String description,
        String venueName,
        String venueAddress,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String coverImageUrl
) {}
