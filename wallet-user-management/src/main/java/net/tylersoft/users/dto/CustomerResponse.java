package net.tylersoft.users.dto;

import net.tylersoft.users.client.CustomerAccount;
import net.tylersoft.users.model.Customer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        String status,
        OffsetDateTime createdAt,
        List<CustomerAccount> accounts
) {
    public static CustomerResponse from(Customer c) {
        return from(c, List.of());
    }

    public static CustomerResponse from(Customer c, List<CustomerAccount> accounts) {
        return new CustomerResponse(
                c.getId(),
                c.getFirstName(),
                c.getLastName(),
                c.getPhoneNumber(),
                c.getEmail(),
                c.getStatus(),
                c.getCreatedAt(),
                accounts
        );
    }
}
