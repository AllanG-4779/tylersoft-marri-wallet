package net.tylersoft.events.repository;

import net.tylersoft.events.model.PromoCode;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PromoCodeRepository extends R2dbcRepository<PromoCode, UUID> {

    Flux<PromoCode> findAllByEventIdOrderByCreatedAtDesc(UUID eventId);

    Mono<Boolean> existsByEventIdAndCode(UUID eventId, String code);
}
