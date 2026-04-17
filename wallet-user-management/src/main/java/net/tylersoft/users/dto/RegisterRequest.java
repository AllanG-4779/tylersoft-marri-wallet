package net.tylersoft.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
        String lastName,

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[1-9]\\d{7,14}$",
                message = "Phone number must be a valid international format, e.g. 254712345678"
        )
        String phoneNumber,

        @NotBlank(message = "Email address is required")
        @Email(message = "Email address is not valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "ID type is required")
        @Pattern(
                regexp = "NATIONAL_ID|PASSPORT|DRIVING_LICENSE",
                message = "idType must be one of: NATIONAL_ID, PASSPORT, DRIVING_LICENSE"
        )
        String idType,

        @Size(max = 100, message = "ID number must not exceed 100 characters")
        String idNumber

) {}
