package net.tylersoft.events.dto.promocode;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdatePromoCodeRequest(
        @NotNull UUID eventId,
        @NotNull UUID id,
        String description,
        BigDecimal discountValue,
        Integer maxUses,
        BigDecimal minOrderAmount,
        OffsetDateTime startsAt,
        OffsetDateTime expiresAt,
        Boolean active
) {}
