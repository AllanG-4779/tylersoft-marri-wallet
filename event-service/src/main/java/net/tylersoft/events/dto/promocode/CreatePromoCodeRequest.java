package net.tylersoft.events.dto.promocode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.tylersoft.events.common.DiscountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePromoCodeRequest(
        @NotNull UUID eventId,
        @NotBlank String code,
        String description,
        @NotNull DiscountType discountType,
        @NotNull BigDecimal discountValue,
        Integer maxUses,
        BigDecimal minOrderAmount,
        OffsetDateTime startsAt,
        OffsetDateTime expiresAt
) {}
