package net.tylersoft.events.dto.tickettype;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ListTicketTypesRequest(@NotNull UUID eventId) {}
