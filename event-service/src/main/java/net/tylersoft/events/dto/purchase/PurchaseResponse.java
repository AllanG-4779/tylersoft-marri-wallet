package net.tylersoft.events.dto.purchase;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseResponse(
        UUID id,
        UUID eventId,
        UUID customerId,
        BigDecimal totalAmount,
        String currency,
        String status,
        String paymentReference,
        List<PurchaseItemResponse> items,
        OffsetDateTime createdAt
) {}
