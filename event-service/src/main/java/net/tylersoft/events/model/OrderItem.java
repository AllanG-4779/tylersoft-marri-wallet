package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("order_items")
@Getter @Setter @NoArgsConstructor
public class OrderItem {

    @Id
    private UUID id;
    private UUID orderId;
    private UUID ticketTypeId;
    private UUID pricingTierId;
    private String itemName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private OffsetDateTime createdAt;
}
