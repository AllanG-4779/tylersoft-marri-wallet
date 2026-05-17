package net.tylersoft.events.dto.media;

import net.tylersoft.events.model.EventMedia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MediaResponse(
        UUID id,
        UUID eventId,
        String mediaType,
        String url,
        String caption,
        int sortOrder,
        OffsetDateTime createdAt
) {
    public static MediaResponse from(EventMedia m) {
        return new MediaResponse(
                m.getId(), m.getEventId(), m.getMediaType(),
                m.getUrl(), m.getCaption(), m.getSortOrder(), m.getCreatedAt()
        );
    }
}
