package net.tylersoft.events.dto.planner;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddPlannerRequest(
        @NotNull UUID customerId,
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        String role
) {}
