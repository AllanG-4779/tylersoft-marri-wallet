package net.tylersoft.events.dto.category;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCategoryRequest(
        UUID parentId,
        @NotBlank String name,
        @NotBlank String slug,
        String iconUrl,
        Integer sortOrder
) {}
