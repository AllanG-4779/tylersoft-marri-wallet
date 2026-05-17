package net.tylersoft.events.dto.tickettype;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTicketTypeRequest(
        @NotNull UUID eventId,
        @NotBlank String name,
        String description,
        String color,
        Integer sortOrder,
        @NotNull @Min(1) Integer totalCapacity,
        @NotNull BigDecimal basePrice,
        Boolean groupTicket,
        Integer groupSize,
        OffsetDateTime salesStartAt,
        OffsetDateTime salesEndAt
) {}
