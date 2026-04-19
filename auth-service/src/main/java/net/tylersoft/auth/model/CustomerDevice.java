package net.tylersoft.auth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(schema = "users", value = "customer_devices")
@Getter
@NoArgsConstructor
public class CustomerDevice {

    @Id
    private UUID id;
    private UUID customerId;
    private String deviceId;
    private String name;
    private String osVersion;
    private String deviceType;
    private String appVersion;
    private String status;
    private OffsetDateTime registeredAt;
    private OffsetDateTime lastSeenAt;
}
