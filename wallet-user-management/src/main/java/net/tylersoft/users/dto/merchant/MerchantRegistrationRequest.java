package net.tylersoft.users.dto.merchant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MerchantRegistrationRequest(

        @NotBlank String businessName,
        @NotBlank @Email String businessEmail,
        @NotBlank String businessPhone,
        @NotBlank String contactPersonName,
        String contactPersonPhone,
        String businessType,
        String registrationNumber,
        String taxNumber,
        String address
) {}
