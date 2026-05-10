package net.tylersoft.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.common.notification.SmsService;
import net.tylersoft.auth.dto.CreateSystemAdminRequest;
import net.tylersoft.auth.dto.SystemAdminCredentialResponse;
import net.tylersoft.auth.dto.SystemAdminResponse;
import net.tylersoft.auth.model.AuthAdmin;
import net.tylersoft.auth.model.AdminRole;
import net.tylersoft.auth.repository.AdminRoleAssignmentRepository;
import net.tylersoft.auth.repository.AdminRoleRepository;
import net.tylersoft.auth.repository.AuthAdminRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemAdminService {

    private final AuthAdminRepository adminRepository;
    private final AdminRoleAssignmentRepository roleAssignmentRepository;
    private final AdminRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;

    public Mono<SystemAdminCredentialResponse> create(CreateSystemAdminRequest req, String createdBy) {
        return Mono.zip(
                adminRepository.existsByUsername(req.username()),
                adminRepository.existsByEmail(req.email())
        ).flatMap(exists -> {
            if (exists.getT1())
                return Mono.error(new IllegalArgumentException("Username already taken"));
            if (exists.getT2())
                return Mono.error(new IllegalArgumentException("Email already registered"));

            String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            AuthAdmin admin = new AuthAdmin();
            admin.setUsername(req.username());
            admin.setEmail(req.email());
            admin.setFirstName(req.firstName());
            admin.setLastName(req.lastName());
            admin.setPhone(req.phone());
            admin.setPasswordHash(passwordEncoder.encode(tempPassword));
            admin.setStatus("ACTIVE");
            admin.setCreatedBy(createdBy);
            admin.setEnabled(true);
            admin.setActive(true);
            admin.setFirstLogin(true);
            admin.setFailedLoginAttempts(0);
            admin.setCredentialsSentAt(OffsetDateTime.now());
            admin.setCreatedOn(OffsetDateTime.now());
            admin.setUpdatedOn(OffsetDateTime.now());

            return adminRepository.save(admin)
                    .doOnSuccess(saved -> {
                        if (saved.getPhone() != null && !saved.getPhone().isBlank()) {
                            smsService.send(saved.getPhone(),
                                    "Welcome. Your admin account has been created. " +
                                    "Username: " + saved.getUsername() + ". " +
                                    "Temporary password: " + tempPassword + ". " +
                                    "Please login and change your password immediately.");
                        }
                    })
                    .map(saved -> new SystemAdminCredentialResponse(
                            SystemAdminResponse.from(saved, List.of()), tempPassword));
        });
    }

    public Mono<Page<SystemAdminResponse>> list(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdOn"));
        return Mono.zip(
                adminRepository.findAllByOrderByCreatedOnDesc(pageable)
                        .flatMap(this::withRoles)
                        .collectList(),
                adminRepository.count()
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    public Mono<SystemAdminResponse> getById(UUID id) {
        return adminRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found")))
                .flatMap(this::withRoles);
    }

    public Mono<SystemAdminResponse> enable(UUID id) {
        return adminRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found")))
                .flatMap(admin -> {
                    admin.setEnabled(true);
                    admin.setActive(true);
                    admin.setStatus("ACTIVE");
                    admin.setAccountLockedUntil(null);
                    admin.setFailedLoginAttempts(0);
                    admin.setUpdatedOn(OffsetDateTime.now());
                    return adminRepository.save(admin);
                })
                .flatMap(this::withRoles);
    }

    public Mono<SystemAdminResponse> disable(UUID id) {
        return adminRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found")))
                .flatMap(admin -> {
                    admin.setEnabled(false);
                    admin.setActive(false);
                    admin.setStatus("INACTIVE");
                    admin.setUpdatedOn(OffsetDateTime.now());
                    return adminRepository.save(admin);
                })
                .flatMap(this::withRoles);
    }

    public Mono<SystemAdminCredentialResponse> resetPassword(UUID id) {
        return adminRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found")))
                .flatMap(admin -> {
                    String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                    admin.setPasswordHash(passwordEncoder.encode(tempPassword));
                    admin.setFirstLogin(true);
                    admin.setFailedLoginAttempts(0);
                    admin.setAccountLockedUntil(null);
                    admin.setCredentialsSentAt(OffsetDateTime.now());
                    admin.setUpdatedOn(OffsetDateTime.now());
                    return adminRepository.save(admin)
                            .flatMap(this::withRoles)
                            .map(resp -> new SystemAdminCredentialResponse(resp, tempPassword));
                });
    }

    private Mono<SystemAdminResponse> withRoles(AuthAdmin admin) {
        return loadRoleNames(admin.getId())
                .map(roles -> SystemAdminResponse.from(admin, roles));
    }

    Mono<List<String>> loadRoleNames(UUID adminId) {
        return roleAssignmentRepository.findByAdminId(adminId)
                .flatMap(a -> roleRepository.findById(a.getRoleId()))
                .map(AdminRole::getName)
                .collectList();
    }
}
