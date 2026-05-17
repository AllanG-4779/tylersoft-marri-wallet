package net.tylersoft.events.dto.tickettype;

import net.tylersoft.events.model.TicketType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketTypeResponse(
        UUID id,
        UUID eventId,
        String name,
        String description,
        String color,
        int sortOrder,
        int totalCapacity,
        int quantitySold,
        int quantityReserved,
        BigDecimal basePrice,
        boolean groupTicket,
        Integer groupSize,
        boolean active,
        boolean hidden,
        OffsetDateTime salesStartAt,
        OffsetDateTime salesEndAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TicketTypeResponse from(TicketType t) {
        return new TicketTypeResponse(
                t.getId(), t.getEventId(), t.getName(), t.getDescription(), t.getColor(),
                t.getSortOrder(), t.getTotalCapacity(), t.getQuantitySold(), t.getQuantityReserved(),
                t.getBasePrice(), t.isGroupTicket(), t.getGroupSize(), t.isActive(), t.isHidden(),
                t.getSalesStartAt(), t.getSalesEndAt(), t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
