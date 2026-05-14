package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


@Table(schema = "events", value = "ticket_purchases")
@Getter @Setter @NoArgsConstructor
public class TicketPurchase {

    @Id
    private UUID id;

    private UUID eventId;
    private UUID customerId;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private String statusReason;
    private OffsetDateTime statusChangedAt;
    private String paymentReference;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
