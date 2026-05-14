package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "events", value = "event_ticket_classes")
@Getter @Setter @NoArgsConstructor
public class EventTicketClass {

    @Id
    private UUID id;

    private UUID eventId;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer capacity;
    private Integer soldCount;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
