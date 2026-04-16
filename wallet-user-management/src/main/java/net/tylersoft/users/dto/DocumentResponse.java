package net.tylersoft.users.dto;

import net.tylersoft.users.model.IdentityDocument;

import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID customerId,
        String idType,
        String idNumber,
        String frontImageUrl,
        String backImageUrl,
        String verificationStatus
) {
    public static DocumentResponse from(IdentityDocument doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getCustomerId(),
                doc.getIdType(),
                doc.getIdNumber(),
                doc.getFrontImageUrl(),
                doc.getBackImageUrl(),
                doc.getVerificationStatus()
        );
    }
}
