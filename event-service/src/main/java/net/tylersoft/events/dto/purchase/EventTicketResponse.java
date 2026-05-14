package net.tylersoft.events.dto.purchase;

import net.tylersoft.events.model.EventTicket;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventTicketResponse(
        UUID id,
        UUID purchaseItemId,
        UUID eventId,
        UUID ticketClassId,
        UUID customerId,
        String ticketCode,
        String status,
        OffsetDateTime issuedAt,
        OffsetDateTime usedAt
) {
    public static EventTicketResponse from(EventTicket t) {
        return new EventTicketResponse(
                t.getId(),
                t.getPurchaseItemId(),
                t.getEventId(),
                t.getTicketClassId(),
                t.getCustomerId(),
                t.getTicketCode(),
                t.getStatus(),
                t.getIssuedAt(),
                t.getUsedAt()
        );
    }
}
