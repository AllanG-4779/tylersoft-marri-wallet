package net.tylersoft.events.dto.validation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ValidationResponse(
        String ticketCode,
        String result,
        String message,
        UUID ticketId,
        UUID eventId,
        OffsetDateTime validatedAt
) {}
