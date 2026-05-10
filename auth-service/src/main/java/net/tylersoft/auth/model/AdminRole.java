package net.tylersoft.auth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("admin_roles")
@Getter @Setter @NoArgsConstructor
public class AdminRole {

    @Id
    private UUID id;
    private String name;
    private String description;
    private OffsetDateTime createdAt;
}
