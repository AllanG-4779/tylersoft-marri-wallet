package net.tylersoft.events.dto.pricingtier;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ListPricingTiersRequest(@NotNull UUID ticketTypeId) {}
