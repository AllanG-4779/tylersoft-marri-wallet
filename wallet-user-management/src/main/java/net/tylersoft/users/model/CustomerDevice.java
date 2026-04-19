package net.tylersoft.users.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "users", value = "customer_devices")
@Getter @Setter @NoArgsConstructor
public class CustomerDevice {

    @Id
    private UUID id;

    private UUID customerId;

    private String deviceId;

    /** Human-readable name — defaults to deviceModel, user can rename. */
    private String name;

    private String osVersion;

    private String deviceType;

    private String appVersion;

    /** ACTIVE | BLOCKED | PENDING */
    private String status;

    private OffsetDateTime registeredAt;

    private OffsetDateTime lastSeenAt;
}
