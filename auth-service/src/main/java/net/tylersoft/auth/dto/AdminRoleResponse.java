package net.tylersoft.auth.dto;

import net.tylersoft.auth.model.AdminRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminRoleResponse(
        UUID id,
        String name,
        String description,
        OffsetDateTime createdAt
) {
    public static AdminRoleResponse from(AdminRole r) {
        return new AdminRoleResponse(r.getId(), r.getName(), r.getDescription(), r.getCreatedAt());
    }
}
