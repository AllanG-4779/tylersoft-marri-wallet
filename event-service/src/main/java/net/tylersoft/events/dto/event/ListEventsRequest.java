package net.tylersoft.events.dto.event;

import net.tylersoft.events.common.EventStatus;

import java.util.UUID;

public record ListEventsRequest(
        UUID organizationId,
        EventStatus status,
        Integer page,
        Integer size
) {}
