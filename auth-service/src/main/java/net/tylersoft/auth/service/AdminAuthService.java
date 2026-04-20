package net.tylersoft.auth.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.auth.dto.AdminLoginRequest;
import net.tylersoft.auth.dto.CreateIntegratorRequest;
import net.tylersoft.auth.dto.CreateIntegratorResponse;
import net.tylersoft.auth.dto.LoginResponse;
import net.tylersoft.auth.model.AuthIntegrator;
import net.tylersoft.auth.repository.AuthAdminRepository;
import net.tylersoft.auth.repository.AuthIntegratorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AuthAdminRepository adminRepository;
    private final AuthIntegratorRepository integratorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public Mono<LoginResponse> login(AdminLoginRequest request) {
        return adminRepository.findByUsername(request.username())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .flatMap(admin -> {
                    if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
                    }
                    if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin account is not active"));
                    }
                    String token = jwtTokenService.issueAdminToken(admin);
                    return Mono.just(LoginResponse.bearer(token, jwtTokenService.expiresInSeconds()));
                });
    }

    public Mono<CreateIntegratorResponse> createIntegrator(CreateIntegratorRequest request, String createdBy) {
        return integratorRepository.existsByName(request.name())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Integrator name already exists"));
                    }
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
}
