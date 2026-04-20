package net.tylersoft.auth.repository;

import net.tylersoft.auth.model.AuthIntegrator;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AuthIntegratorRepository extends ReactiveCrudRepository<AuthIntegrator, UUID> {

    Mono<AuthIntegrator> findByAccessKey(String accessKey);

    Mono<Boolean> existsByName(String name);
}
