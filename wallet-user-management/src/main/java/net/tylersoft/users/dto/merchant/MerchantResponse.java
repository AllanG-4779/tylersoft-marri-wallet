package net.tylersoft.users.dto.merchant;

import net.tylersoft.users.model.Merchant;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String merchantCode,
        String businessName,
        String businessEmail,
        String businessPhone,
        String contactPersonName,
        String contactPersonPhone,
        String businessType,
        String registrationNumber,
        String taxNumber,
        String address,
        String status,
        String statusReason,
        String accountNumber,
        OffsetDateTime approvedAt,
        OffsetDateTime createdAt
) {
    public static MerchantResponse from(Merchant m) {
        return new MerchantResponse(
                m.getId(),
                m.getMerchantCode(),
                m.getBusinessName(),
                m.getBusinessEmail(),
                m.getBusinessPhone(),
                m.getContactPersonName(),
                m.getContactPersonPhone(),
                m.getBusinessType(),
                m.getRegistrationNumber(),
                m.getTaxNumber(),
                m.getAddress(),
                m.getStatus(),
                m.getStatusReason(),
                m.getAccountNumber(),
                m.getApprovedAt(),
                m.getCreatedAt()
        );
    }
}
