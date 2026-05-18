package net.tylersoft.events.repository;

import net.tylersoft.events.model.TicketType;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TicketTypeRepository extends R2dbcRepository<TicketType, UUID> {

    Flux<TicketType> findAllByEventIdOrderBySortOrderAsc(UUID eventId);

    Mono<Boolean> existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    Mono<Boolean> existsByEventIdAndNameIgnoreCaseAndIdNot(UUID eventId, String name, UUID id);
}
