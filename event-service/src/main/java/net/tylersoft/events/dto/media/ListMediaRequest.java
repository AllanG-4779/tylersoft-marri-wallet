package net.tylersoft.events.dto.media;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ListMediaRequest(@NotNull UUID eventId) {}
