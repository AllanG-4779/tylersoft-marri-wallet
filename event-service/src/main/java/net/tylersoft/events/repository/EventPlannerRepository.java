package net.tylersoft.events.repository;

import net.tylersoft.events.model.EventPlanner;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EventPlannerRepository extends R2dbcRepository<EventPlanner, UUID> {

    Flux<EventPlanner> findAllByEventId(UUID eventId);

    Mono<Boolean> existsByEventIdAndCustomerId(UUID eventId, UUID customerId);

    Mono<EventPlanner> findByEventIdAndCustomerId(UUID eventId, UUID customerId);

    Flux<EventPlanner> findAllByCustomerIdAndStatus(UUID customerId, String status);
}
