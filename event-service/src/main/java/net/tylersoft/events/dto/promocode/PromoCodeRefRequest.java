package net.tylersoft.events.dto.promocode;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PromoCodeRefRequest(
        @NotNull UUID eventId,
        @NotNull UUID id
) {}
