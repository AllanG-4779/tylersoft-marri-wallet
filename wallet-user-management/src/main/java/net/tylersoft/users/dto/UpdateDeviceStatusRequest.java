package net.tylersoft.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateDeviceStatusRequest(
        @NotBlank
        @Pattern(regexp = "ACTIVE|BLOCKED|PENDING", message = "status must be ACTIVE, BLOCKED, or PENDING")
        String status
) {}
