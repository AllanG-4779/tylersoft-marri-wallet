package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("ticket_types")
@Getter @Setter @NoArgsConstructor
public class TicketType {

    @Id
    private UUID id;
    private UUID eventId;
    private String name;
    private String description;
    private String color;
    private int sortOrder;
    private int totalCapacity;
    private int quantitySold;
    private int quantityReserved;
    private BigDecimal basePrice;
    private boolean isGroupTicket;
    private Integer groupSize;
    private boolean isActive;
    private boolean isHidden;
    private OffsetDateTime salesStartAt;
    private OffsetDateTime salesEndAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
