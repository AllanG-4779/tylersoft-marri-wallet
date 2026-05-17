package net.tylersoft.events.dto.category;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateCategoryRequest(
        @NotNull UUID id,
        String name,
        String slug,
        String iconUrl,
        Integer sortOrder,
        Boolean active
) {}
