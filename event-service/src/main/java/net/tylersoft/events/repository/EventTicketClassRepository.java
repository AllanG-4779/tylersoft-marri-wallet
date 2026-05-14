package net.tylersoft.events.repository;

import net.tylersoft.events.model.EventTicketClass;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface EventTicketClassRepository extends R2dbcRepository<EventTicketClass, UUID> {

    Flux<EventTicketClass> findAllByEventId(UUID eventId);

    Flux<EventTicketClass> findAllByEventIdAndStatus(UUID eventId, String status);
}
