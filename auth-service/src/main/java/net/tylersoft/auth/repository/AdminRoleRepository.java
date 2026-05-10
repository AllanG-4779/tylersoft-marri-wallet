package net.tylersoft.auth.repository;

import net.tylersoft.auth.model.AdminRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AdminRoleRepository extends ReactiveCrudRepository<AdminRole, UUID> {

    Mono<Boolean> existsByName(String name);
}
