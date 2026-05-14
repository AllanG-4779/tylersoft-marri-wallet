package net.tylersoft.events.dto.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreatePurchaseRequest(
        @NotNull UUID eventId,
        @NotEmpty @Valid List<PurchaseItemRequest> items
) {}
