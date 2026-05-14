package net.tylersoft.events.repository;

import net.tylersoft.events.model.TicketValidation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TicketValidationRepository extends R2dbcRepository<TicketValidation, UUID> {

    Flux<TicketValidation> findAllByTicketCode(String ticketCode);

    Flux<TicketValidation> findAllByEventIdOrderByValidatedAtDesc(UUID eventId, Pageable pageable);

    Flux<TicketValidation> findAllByEventIdAndResult(UUID eventId, String result);

    Mono<Long> countByEventId(UUID eventId);
}
