package net.tylersoft.auth.dto;

public record SystemAdminCredentialResponse(
        SystemAdminResponse admin,
        String temporaryPassword
) {}
