package net.tylersoft.auth.dto;

import java.util.UUID;

public record CreateIntegratorResponse(
        UUID id,
        String name,
        String accessKey,
        String accessSecret
) {}
