package net.tylersoft.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResendOtpRequest(

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^\\+?[1-9]\\d{7,14}$",
                message = "Phone number must be a valid international format"
        )
        String phoneNumber,

        @NotBlank(message = "Purpose is required")
        @Pattern(
                regexp = "REGISTRATION|PIN_RESET|LOGIN",
                message = "Purpose must be one of: REGISTRATION, PIN_RESET, LOGIN"
        )
        String purpose

) {}
