package net.tylersoft.events.dto.event;

import jakarta.validation.constraints.NotNull;
import net.tylersoft.events.common.EventAction;

import java.util.UUID;

public record EventStatusRequest(
        @NotNull UUID id,
        @NotNull EventAction action,
        String notes
) {}
