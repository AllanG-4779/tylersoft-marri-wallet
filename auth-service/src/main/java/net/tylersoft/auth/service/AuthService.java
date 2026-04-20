package net.tylersoft.auth.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.auth.dto.IntegratorLoginRequest;
import net.tylersoft.auth.dto.LoginRequest;
import net.tylersoft.auth.dto.LoginResponse;
import net.tylersoft.auth.repository.AuthCustomerRepository;
import net.tylersoft.auth.repository.AuthIntegratorRepository;
import net.tylersoft.auth.repository.CustomerDeviceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthCustomerRepository customerRepository;
    private final CustomerDeviceRepository deviceRepository;
    private final AuthIntegratorRepository integratorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public Mono<LoginResponse> integratorLogin(IntegratorLoginRequest request) {
        return integratorRepository.findByAccessKey(request.accessKey())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .flatMap(integrator -> {
                    if (!passwordEncoder.matches(request.accessSecret(), integrator.getSecretHash())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
                    }
                    if (!"ACTIVE".equalsIgnoreCase(integrator.getStatus())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Integrator is not active"));
                    }
                    String token = jwtTokenService.issueIntegratorToken(integrator);
                    return Mono.just(LoginResponse.bearer(token, jwtTokenService.expiresInSeconds()));
                });
    }

    public Mono<LoginResponse> login(LoginRequest request, String deviceId) {
        return customerRepository.findByPhoneNumber(request.phoneNumber())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .flatMap(customer -> {
                    if (!passwordEncoder.matches(request.pin(), customer.getPinHash())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
                    }
                    if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN, "Account is not active: " + customer.getStatus()));
                    }
                    return deviceRepository.findByCustomerIdAndDeviceId(customer.getId(), deviceId)
                            .switchIfEmpty(Mono.error(new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED,
                                    "Device not registered. Complete onboarding from this device first.")))
                            .flatMap(device -> {
                                if ("BLOCKED".equalsIgnoreCase(device.getStatus())) {
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.FORBIDDEN, "This device has been blocked."));
                                }
                                if (!"ACTIVE".equalsIgnoreCase(device.getStatus())) {
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.FORBIDDEN, "Device status is: " + device.getStatus()));
                                }
                                return deviceRepository.updateLastSeenAt(device.getId(), OffsetDateTime.now())
                                        .thenReturn(customer);
                            });
                })
                .map(customer -> {
                    String token = jwtTokenService.issueCustomerToken(customer, deviceId);
                    return LoginResponse.bearer(token, jwtTokenService.expiresInSeconds());
                });
    }
}
