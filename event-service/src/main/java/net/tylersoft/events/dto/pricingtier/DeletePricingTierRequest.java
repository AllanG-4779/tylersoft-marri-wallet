package net.tylersoft.events.dto.pricingtier;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeletePricingTierRequest(
        @NotNull UUID ticketTypeId,
        @NotNull UUID id
) {}
