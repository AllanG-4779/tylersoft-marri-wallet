package net.tylersoft.events.dto.pricingtier;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdatePricingTierRequest(
        @NotNull UUID id,
        String name,
        BigDecimal price,
        Integer quantity,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer sortOrder
) {}
