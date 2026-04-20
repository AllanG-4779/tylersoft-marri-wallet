package net.tylersoft.auth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("auth_integrators")
@Getter @Setter @NoArgsConstructor
public class AuthIntegrator {

    @Id
    private UUID id;
    private String name;
    private String accessKey;
    private String secretHash;
    private String description;
    private String status;
    private String createdBy;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
}
