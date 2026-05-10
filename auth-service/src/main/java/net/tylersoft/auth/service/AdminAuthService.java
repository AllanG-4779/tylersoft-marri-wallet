package net.tylersoft.auth.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.auth.dto.AdminLoginRequest;
import net.tylersoft.auth.dto.CreateIntegratorRequest;
import net.tylersoft.auth.dto.CreateIntegratorResponse;
import net.tylersoft.auth.dto.LoginResponse;
import net.tylersoft.auth.model.AdminRole;
import net.tylersoft.auth.model.AuthIntegrator;
import net.tylersoft.auth.repository.AdminRoleAssignmentRepository;
import net.tylersoft.auth.repository.AdminRoleRepository;
import net.tylersoft.auth.repository.AuthAdminRepository;
import net.tylersoft.auth.repository.AuthIntegratorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AuthAdminRepository adminRepository;
    private final AuthIntegratorRepository integratorRepository;
    private final AdminRoleAssignmentRepository roleAssignmentRepository;
    private final AdminRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public Mono<LoginResponse> login(AdminLoginRequest request) {
        return adminRepository.findByUsername(request.username())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .flatMap(admin -> {
                    if (Boolean.FALSE.equals(admin.getEnabled()) || Boolean.FALSE.equals(admin.getActive())
                            || !"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin account is not active"));
                    }
                    if (admin.getAccountLockedUntil() != null
                            && admin.getAccountLockedUntil().isAfter(OffsetDateTime.now())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Account is locked until " + admin.getAccountLockedUntil()));
                    }
                    if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
                        int attempts = (admin.getFailedLoginAttempts() == null ? 0 : admin.getFailedLoginAttempts()) + 1;
                        admin.setFailedLoginAttempts(attempts);
                        if (attempts >= MAX_FAILED_ATTEMPTS) {
                            admin.setAccountLockedUntil(OffsetDateTime.now().plusMinutes(15));
                        }
                        admin.setUpdatedOn(OffsetDateTime.now());
                        return adminRepository.save(admin)
                                .then(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")));
                    }
                    admin.setFailedLoginAttempts(0);
                    admin.setAccountLockedUntil(null);
                    admin.setLastLoginAt(OffsetDateTime.now());
                    admin.setUpdatedOn(OffsetDateTime.now());
                    return adminRepository.save(admin)
                            .flatMap(saved -> loadRoleNames(saved.getId())
                                    .map(roles -> {
                                        String token = jwtTokenService.issueAdminToken(saved, roles);
                                        return LoginResponse.bearer(token, jwtTokenService.expiresInSeconds());
                                    }));
                });
    }

    public Mono<CreateIntegratorResponse> createIntegrator(CreateIntegratorRequest request, String createdBy) {
        return integratorRepository.existsByName(request.name())
                .flatMap(exists -> {
                    if (exists)
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Integrator name already exists"));
                    String accessKey = UUID.randomUUID().toString().replace("-", "");
                    String plainSecret = UUID.randomUUID().toString().replace("-", "")
                            + UUID.randomUUID().toString().replace("-", "");

                    AuthIntegrator integrator = new AuthIntegrator();
                    integrator.setName(request.name());
                    integrator.setAccessKey(accessKey);
                    integrator.setSecretHash(passwordEncoder.encode(plainSecret));
                    integrator.setDescription(request.description());
                    integrator.setStatus("ACTIVE");
                    integrator.setCreatedBy(createdBy);
                    integrator.setCreatedOn(OffsetDateTime.now());
                    integrator.setUpdatedOn(OffsetDateTime.now());

                    return integratorRepository.save(integrator)
                            .map(saved -> new CreateIntegratorResponse(
                                    saved.getId(), saved.getName(), saved.getAccessKey(), plainSecret));
                });
    }

    private Mono<List<String>> loadRoleNames(UUID adminId) {
        return roleAssignmentRepository.findByAdminId(adminId)
                .flatMap(a -> roleRepository.findById(a.getRoleId()))
                .map(AdminRole::getName)
                .collectList();
    }
}
