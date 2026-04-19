package net.tylersoft.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record SetPinRequest(

        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "^\\d{4,6}$", message = "PIN must be 4 to 6 digits")
        String pin,
        UUID customerId

) {}
