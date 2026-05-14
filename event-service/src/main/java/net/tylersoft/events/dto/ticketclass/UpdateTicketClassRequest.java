package net.tylersoft.events.dto.ticketclass;

import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateTicketClassRequest(
        String name,
        String description,
        BigDecimal price,
        @Min(1) Integer capacity
) {}
