package net.tylersoft.events.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.tylersoft.events.common.OrderStatus;
import net.tylersoft.events.common.PaymentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("orders")
@Getter @Setter @NoArgsConstructor
public class Order {

    @Id
    private UUID id;
    private UUID eventId;
    private UUID organizationId;
    private UUID customerId;
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String customerPhone;
    private String orderNumber;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String currency;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String paymentReference;
    private OffsetDateTime paidAt;
    private UUID promoCodeId;
    private boolean isGroupOrder;
    private String notes;
    private OffsetDateTime refundedAt;
    private String refundReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
