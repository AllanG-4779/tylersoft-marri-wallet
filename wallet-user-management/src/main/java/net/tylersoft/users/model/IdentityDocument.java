package net.tylersoft.users.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "users", value = "identity_documents")
@Getter @Setter @NoArgsConstructor
public class IdentityDocument {

    @Id
    private UUID id;

    private UUID customerId;
    private String idType;
    private String idNumber;
    private String frontImageUrl;
    private String backImageUrl;
    private String verificationStatus;
    private String rejectionReason;
    private String providerReference;
    private OffsetDateTime verifiedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
