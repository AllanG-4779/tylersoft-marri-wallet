package net.tylersoft.users.dto;

import net.tylersoft.users.client.CustomerAccount;
import net.tylersoft.users.model.Customer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminCustomerDetailResponse(
        UUID id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        String status,
        String statusReason,
        OffsetDateTime statusChangedAt,
        OffsetDateTime createdAt,
        List<CustomerAccount> accounts,
        List<DocumentResponse> documents,
        List<DeviceResponse> devices
) {
    public static AdminCustomerDetailResponse from(Customer c,
                                                   List<CustomerAccount> accounts,
                                                   List<DocumentResponse> documents,
                                                   List<DeviceResponse> devices) {
        return new AdminCustomerDetailResponse(
                c.getId(), c.getFirstName(), c.getLastName(),
                c.getPhoneNumber(), c.getEmail(), c.getStatus(),
                c.getStatusReason(), c.getStatusChangedAt(), c.getCreatedAt(),
                accounts, documents, devices
        );
    }
}