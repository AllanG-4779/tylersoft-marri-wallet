package net.tylersoft.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateIntegratorRequest(
        @NotBlank String name,
        String description
) {}
