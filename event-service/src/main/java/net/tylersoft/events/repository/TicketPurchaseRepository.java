package net.tylersoft.events.repository;

import net.tylersoft.events.model.TicketPurchase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TicketPurchaseRepository extends R2dbcRepository<TicketPurchase, UUID> {

    Flux<TicketPurchase> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Flux<TicketPurchase> findAllByEventIdOrderByCreatedAtDesc(UUID eventId, Pageable pageable);

    Mono<Long> countByEventId(UUID eventId);

    Mono<TicketPurchase> findByIdAndCustomerId(UUID id, UUID customerId);

    Mono<Long> countByCustomerId(UUID customerId);
}
