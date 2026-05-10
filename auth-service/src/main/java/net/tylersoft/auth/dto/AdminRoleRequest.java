package net.tylersoft.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRoleRequest(
        @NotBlank @Size(max = 100) String name,
        String description
) {}
