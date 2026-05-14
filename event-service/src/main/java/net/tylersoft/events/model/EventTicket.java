package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "events", value = "event_tickets")
@Getter @Setter @NoArgsConstructor
public class EventTicket {

    @Id
    private UUID id;

    private UUID purchaseItemId;
    private UUID eventId;
    private UUID ticketClassId;
    private UUID customerId;
    private String ticketCode;
    private String status;
    private OffsetDateTime issuedAt;
    private OffsetDateTime usedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
