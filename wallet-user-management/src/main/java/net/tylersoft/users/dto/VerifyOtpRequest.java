package net.tylersoft.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^\\+?[1-9]\\d{7,14}$",
                message = "Phone number must be a valid international format"
        )
        String phoneNumber,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
        String otp,

        @NotBlank(message = "Purpose is required")
        @Pattern(
                regexp = "REGISTRATION|PIN_RESET|LOGIN",
                message = "Purpose must be one of: REGISTRATION, PIN_RESET, LOGIN"
        )
        String purpose

) {}
