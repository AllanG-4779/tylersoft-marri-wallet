package net.tylersoft.users.dto;

import net.tylersoft.users.model.Customer;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        String status,
        OffsetDateTime createdAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getFirstName(),
                c.getLastName(),
                c.getPhoneNumber(),
                c.getEmail(),
                c.getStatus(),
                c.getCreatedAt()
        );
    }
}
