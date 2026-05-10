package net.tylersoft.auth.repository;

import net.tylersoft.auth.model.AdminRoleAssignment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AdminRoleAssignmentRepository extends ReactiveCrudRepository<AdminRoleAssignment, UUID> {

    Flux<AdminRoleAssignment> findByAdminId(UUID adminId);
//    Mono<AdminRoleAssignment> findByAdminIdAndRoleId(UUID adminId, UUID roleId);
    Mono<Void> deleteByAdminIdAndRoleId(UUID adminId, UUID roleId);
    Mono<Boolean> existsByAdminIdAndRoleId(UUID adminId, UUID roleId);
}
