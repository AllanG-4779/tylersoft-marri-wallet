package net.tylersoft.events.dto.pricingtier;

import net.tylersoft.events.common.TicketTierStatus;
import net.tylersoft.events.model.PricingTier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PricingTierResponse(
        UUID id,
        UUID ticketTypeId,
        String name,
        BigDecimal price,
        int quantity,
        int quantitySold,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        TicketTierStatus status,
        int sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PricingTierResponse from(PricingTier p) {
        return new PricingTierResponse(
                p.getId(), p.getTicketTypeId(), p.getName(), p.getPrice(),
                p.getQuantity(), p.getQuantitySold(), p.getStartsAt(), p.getEndsAt(),
                p.getStatus(), p.getSortOrder(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
