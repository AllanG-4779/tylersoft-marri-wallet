package net.tylersoft.auth.repository;

import net.tylersoft.auth.model.AuthAdmin;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AuthAdminRepository extends ReactiveCrudRepository<AuthAdmin, UUID> {

    Mono<AuthAdmin> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);
}
