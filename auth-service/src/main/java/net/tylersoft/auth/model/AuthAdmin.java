package net.tylersoft.auth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("auth_admins")
@Getter @Setter @NoArgsConstructor
public class AuthAdmin {

    @Id
    private UUID id;
    private String username;
    private String passwordHash;
    private String status;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
}
