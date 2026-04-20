package net.tylersoft.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record IntegratorLoginRequest(
        @NotBlank String accessKey,
        @NotBlank String accessSecret
) {}
