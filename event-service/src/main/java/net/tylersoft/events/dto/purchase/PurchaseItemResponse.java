package net.tylersoft.events.dto.purchase;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemResponse(
        UUID id,
        UUID ticketClassId,
        String ticketClassName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
