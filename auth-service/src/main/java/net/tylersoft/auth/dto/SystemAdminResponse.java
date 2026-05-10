package net.tylersoft.auth.dto;

import net.tylersoft.auth.model.AuthAdmin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SystemAdminResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String createdBy,
        Boolean enabled,
        Boolean active,
        Boolean firstLogin,
        List<String> roles,
        OffsetDateTime credentialsSentAt,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdOn
) {
    public static SystemAdminResponse from(AuthAdmin a, List<String> roles) {
        return new SystemAdminResponse(
                a.getId(),
                a.getUsername(),
                a.getEmail(),
                a.getFirstName(),
                a.getLastName(),
                a.getPhone(),
                a.getCreatedBy(),
                a.getEnabled(),
                a.getActive(),
                a.getFirstLogin(),
                roles,
                a.getCredentialsSentAt(),
                a.getLastLoginAt(),
                a.getCreatedOn()
        );
    }
}
