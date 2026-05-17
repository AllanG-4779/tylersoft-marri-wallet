package net.tylersoft.events.dto.promocode;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ListPromoCodesRequest(@NotNull UUID eventId) {}
