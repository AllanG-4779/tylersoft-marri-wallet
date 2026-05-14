package net.tylersoft.events.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CreateEventRequest(
        @NotBlank String merchantCode,
        @NotBlank String title,
        String description,
        String venueName,
        String venueAddress,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt,
        String coverImageUrl
) {}
