package net.tylersoft.users.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "users", value = "merchant_documents")
@Getter @Setter @NoArgsConstructor
public class MerchantDocument {

    @Id
    private UUID id;

    private UUID merchantId;
    private String documentType;
    private String documentUrl;
    private String verificationStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
