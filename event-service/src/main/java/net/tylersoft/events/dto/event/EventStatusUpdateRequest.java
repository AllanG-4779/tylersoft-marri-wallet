package net.tylersoft.events.dto.event;

import jakarta.validation.constraints.NotBlank;

public record EventStatusUpdateRequest(
        @NotBlank String action,
        String reason
) {}
