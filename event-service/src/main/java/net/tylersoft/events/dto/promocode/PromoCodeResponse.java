package net.tylersoft.events.dto.promocode;

import net.tylersoft.events.common.DiscountType;
import net.tylersoft.events.model.PromoCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PromoCodeResponse(
        UUID id,
        UUID eventId,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        Integer maxUses,
        int usesCount,
        BigDecimal minOrderAmount,
        OffsetDateTime startsAt,
        OffsetDateTime expiresAt,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PromoCodeResponse from(PromoCode p) {
        return new PromoCodeResponse(
                p.getId(), p.getEventId(), p.getCode(), p.getDescription(),
                p.getDiscountType(), p.getDiscountValue(), p.getMaxUses(), p.getUsesCount(),
                p.getMinOrderAmount(), p.getStartsAt(), p.getExpiresAt(), p.isActive(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
