package net.tylersoft.users.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "users", value = "customers")
@Getter @Setter @NoArgsConstructor
public class Customer {

    @Id
    private UUID id;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String pinHash;
    private String status;
    private String statusReason;
    private OffsetDateTime statusChangedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
