package net.tylersoft.events.repository;

import net.tylersoft.events.model.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EventRepository extends R2dbcRepository<Event, UUID> {

    Flux<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Flux<Event> findAllByMerchantCodeOrderByCreatedAtDesc(String merchantCode, Pageable pageable);

    Mono<Long> countByMerchantCode(String merchantCode);

    Flux<Event> findAllByStatusOrderByStartAtAsc(String status, Pageable pageable);

    Mono<Long> countByStatus(String status);
}
