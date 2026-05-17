package net.tylersoft.events.repository;

import net.tylersoft.events.model.EventMedia;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface EventMediaRepository extends R2dbcRepository<EventMedia, UUID> {

    Flux<EventMedia> findAllByEventIdOrderBySortOrderAsc(UUID eventId);
}
