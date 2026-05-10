package net.tylersoft.auth.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.auth.dto.AdminRoleRequest;
import net.tylersoft.auth.dto.AdminRoleResponse;
import net.tylersoft.auth.model.AdminRoleAssignment;
import net.tylersoft.auth.repository.AdminRoleAssignmentRepository;
import net.tylersoft.auth.repository.AdminRoleRepository;
import net.tylersoft.auth.repository.AuthAdminRepository;
import net.tylersoft.auth.model.AdminRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final AdminRoleRepository roleRepository;
    private final AdminRoleAssignmentRepository assignmentRepository;
    private final AuthAdminRepository adminRepository;

    public Mono<AdminRoleResponse> create(AdminRoleRequest req) {
        return roleRepository.existsByName(req.name())
                .flatMap(exists -> {
                    if (exists)
                        return Mono.error(new IllegalArgumentException("Role already exists: " + req.name()));
                    AdminRole role = new AdminRole();
                    role.setName(req.name().toUpperCase());
                    role.setDescription(req.description());
                    role.setCreatedAt(OffsetDateTime.now());
                    return roleRepository.save(role);
                })
                .map(AdminRoleResponse::from);
    }

    public Flux<AdminRoleResponse> listAll() {
        return roleRepository.findAll().map(AdminRoleResponse::from);
    }

    public Mono<Void> delete(UUID roleId) {
        return roleRepository.findById(roleId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found")))
                .flatMap(role -> roleRepository.deleteById(roleId));
    }

    public Mono<Void> assign(UUID adminId, UUID roleId) {
        return Mono.zip(
                adminRepository.findById(adminId)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"))),
                roleRepository.findById(roleId)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found")))
        ).flatMap(t ->
                assignmentRepository.existsByAdminIdAndRoleId(adminId, roleId)
                        .flatMap(exists -> {
                            if (exists) return Mono.empty();
                            AdminRoleAssignment a = new AdminRoleAssignment();
                            a.setAdminId(adminId);
                            a.setRoleId(roleId);
                            return assignmentRepository.save(a).then();
                        })
        );
    }

    public Mono<Void> remove(UUID adminId, UUID roleId) {
        return assignmentRepository.existsByAdminIdAndRoleId(adminId, roleId)
                .flatMap(exists -> {
                    if (!exists)
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
                    return assignmentRepository.deleteByAdminIdAndRoleId(adminId, roleId);
                });
    }
}
