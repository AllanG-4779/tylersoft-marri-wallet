package net.tylersoft.events.dto.ticketclass;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTicketClassRequest(
        @NotBlank String name,
        String description,
        @NotNull BigDecimal price,
        String currency,
        @NotNull @Min(1) Integer capacity
) {}
