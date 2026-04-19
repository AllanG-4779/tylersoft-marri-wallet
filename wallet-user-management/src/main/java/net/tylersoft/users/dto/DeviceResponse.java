package net.tylersoft.users.dto;

import net.tylersoft.users.model.CustomerDevice;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String deviceId,
        String name,
        String osVersion,
        String deviceType,
        String appVersion,
        String status,
        OffsetDateTime registeredAt,
        OffsetDateTime lastSeenAt
) {
    public static DeviceResponse from(CustomerDevice d) {
        return new DeviceResponse(
                d.getId(), d.getDeviceId(), d.getName(),
                d.getOsVersion(), d.getDeviceType(), d.getAppVersion(),
                d.getStatus(), d.getRegisteredAt(), d.getLastSeenAt()
        );
    }
}
