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
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String passwordHash;
    private String status;
    private String createdBy;
    private Boolean enabled;
    private Boolean active;
    private Boolean firstLogin;
    private OffsetDateTime credentialsSentAt;
    private OffsetDateTime lastLoginAt;
    private Integer failedLoginAttempts;
    private OffsetDateTime accountLockedUntil;
    private OffsetDateTime createdOn;
    private OffsetDateTime updatedOn;
}
