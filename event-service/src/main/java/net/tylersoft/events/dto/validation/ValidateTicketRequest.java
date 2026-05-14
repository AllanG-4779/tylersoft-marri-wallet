package net.tylersoft.events.dto.validation;

import jakarta.validation.constraints.NotBlank;

public record ValidateTicketRequest(
        @NotBlank String ticketCode
) {}
