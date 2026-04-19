package net.tylersoft.auth.repository;

import net.tylersoft.auth.model.CustomerDevice;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface CustomerDeviceRepository extends ReactiveCrudRepository<CustomerDevice, UUID> {

    Mono<CustomerDevice> findByCustomerIdAndDeviceId(UUID customerId, String deviceId);

    @Modifying
    @Query("UPDATE users.customer_devices SET last_seen_at = :lastSeenAt WHERE id = :id")
    Mono<Void> updateLastSeenAt(UUID id, OffsetDateTime lastSeenAt);
}
