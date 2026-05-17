package net.tylersoft.events.dto.pricingtier;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePricingTierRequest(
        @NotNull UUID ticketTypeId,
        @NotBlank String name,
        @NotNull BigDecimal price,
        @NotNull @Min(1) Integer quantity,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        Integer sortOrder
) {}
