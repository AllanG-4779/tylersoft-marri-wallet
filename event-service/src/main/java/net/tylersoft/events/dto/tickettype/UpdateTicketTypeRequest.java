package net.tylersoft.events.dto.tickettype;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateTicketTypeRequest(
        @NotNull UUID id,
        String name,
        String description,
        String color,
        Integer sortOrder,
        Integer totalCapacity,
        BigDecimal basePrice,
        Boolean groupTicket,
        Integer groupSize,
        Boolean active,
        Boolean hidden,
        OffsetDateTime salesStartAt,
        OffsetDateTime salesEndAt
) {}
