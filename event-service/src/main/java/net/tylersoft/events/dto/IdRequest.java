package net.tylersoft.events.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record IdRequest(@NotNull UUID id) {}
