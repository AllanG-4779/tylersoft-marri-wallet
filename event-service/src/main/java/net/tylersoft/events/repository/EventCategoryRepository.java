package net.tylersoft.events.repository;

import net.tylersoft.events.model.EventCategory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface EventCategoryRepository extends R2dbcRepository<EventCategory, UUID> {

    Flux<EventCategory> findAllByParentIdIsNullOrderBySortOrderAsc();

    Flux<EventCategory> findAllByParentIdOrderBySortOrderAsc(UUID parentId);
}
