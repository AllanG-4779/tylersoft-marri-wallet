package net.tylersoft.events.dto.tag;

import net.tylersoft.events.model.Tag;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TagResponse(
        UUID id,
        String name,
        String slug,
        OffsetDateTime createdAt
) {
    public static TagResponse from(Tag t) {
        return new TagResponse(t.getId(), t.getName(), t.getSlug(), t.getCreatedAt());
    }
}
