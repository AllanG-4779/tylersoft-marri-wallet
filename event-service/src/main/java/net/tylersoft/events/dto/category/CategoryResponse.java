package net.tylersoft.events.dto.category;

import net.tylersoft.events.model.EventCategory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        UUID parentId,
        String name,
        String slug,
        String iconUrl,
        int sortOrder,
        boolean active,
        OffsetDateTime createdAt
) {
    public static CategoryResponse from(EventCategory c) {
        return new CategoryResponse(
                c.getId(), c.getParentId(), c.getName(), c.getSlug(),
                c.getIconUrl(), c.getSortOrder(), c.isActive(), c.getCreatedAt()
        );
    }
}
