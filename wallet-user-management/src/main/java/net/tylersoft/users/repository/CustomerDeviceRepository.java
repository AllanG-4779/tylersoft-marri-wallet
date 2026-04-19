package net.tylersoft.users.repository;

import net.tylersoft.users.model.CustomerDevice;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerDeviceRepository extends ReactiveCrudRepository<CustomerDevice, UUID> {

    Flux<CustomerDevice> findByCustomerId(UUID customerId);

    Mono<CustomerDevice> findByCustomerIdAndDeviceId(UUID customerId, String deviceId);
}
