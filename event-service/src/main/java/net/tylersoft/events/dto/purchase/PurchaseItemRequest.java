package net.tylersoft.events.dto.purchase;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PurchaseItemRequest(
        @NotNull UUID ticketClassId,
        @NotNull @Min(1) Integer quantity
) {}
