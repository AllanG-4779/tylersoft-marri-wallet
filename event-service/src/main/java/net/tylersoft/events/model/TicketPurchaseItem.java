package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "events", value = "ticket_purchase_items")
@Getter @Setter @NoArgsConstructor
public class TicketPurchaseItem {

    @Id
    private UUID id;

    private UUID purchaseId;
    private UUID ticketClassId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private OffsetDateTime createdAt;
}
