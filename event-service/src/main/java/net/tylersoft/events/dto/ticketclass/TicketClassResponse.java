package net.tylersoft.events.dto.ticketclass;

import net.tylersoft.events.model.EventTicketClass;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketClassResponse(
        UUID id,
        UUID eventId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int capacity,
        int soldCount,
        int available,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TicketClassResponse from(EventTicketClass tc) {
        return new TicketClassResponse(
                tc.getId(),
                tc.getEventId(),
                tc.getName(),
                tc.getDescription(),
                tc.getPrice(),
                tc.getCurrency(),
                tc.getCapacity(),
                tc.getSoldCount(),
                tc.getCapacity() - tc.getSoldCount(),
                tc.getStatus(),
                tc.getCreatedAt(),
                tc.getUpdatedAt()
        );
    }
}
