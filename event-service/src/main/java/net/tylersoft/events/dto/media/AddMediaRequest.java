package net.tylersoft.events.dto.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AddMediaRequest(
        @NotNull UUID eventId,
        @NotBlank @Pattern(regexp = "image|video") String mediaType,
        @NotBlank String url,
        String caption,
        Integer sortOrder
) {}
