package net.tylersoft.auth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("admin_role_assignments")
@Getter @Setter @NoArgsConstructor
public class AdminRoleAssignment {

    @Id
    private UUID id;
    private UUID adminId;
    private UUID roleId;
}
