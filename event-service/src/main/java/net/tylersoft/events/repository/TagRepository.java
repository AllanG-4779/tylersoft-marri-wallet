package net.tylersoft.events.repository;

import net.tylersoft.events.model.Tag;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TagRepository extends R2dbcRepository<Tag, UUID> {

    Flux<Tag> findAllByOrderByNameAsc();

    Mono<Boolean> existsBySlug(String slug);
}
