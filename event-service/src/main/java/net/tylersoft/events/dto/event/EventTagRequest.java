package net.tylersoft.events.dto.event;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EventTagRequest(
        @NotNull UUID eventId,
        @NotNull UUID tagId
) {}
