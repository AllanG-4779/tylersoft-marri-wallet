package net.tylersoft.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.auth.dto.IntegratorLoginRequest;
import net.tylersoft.auth.dto.LoginRequest;
import net.tylersoft.auth.dto.LoginResponse;
import net.tylersoft.auth.service.AuthService;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Mono<ApiResponse<LoginResponse>> login(
            @Validated @RequestBody UniversalRequestWrapper<LoginRequest> request) {
        return authService.login(request.data(), request.channelDetails().deviceId())
                .map(ApiResponse::ok);
    }

    @PostMapping("/integrator/login")
    public Mono<ApiResponse<LoginResponse>> integratorLogin(
            @Validated @RequestBody UniversalRequestWrapper<IntegratorLoginRequest> request) {
        return authService.integratorLogin(request.data())
                .map(ApiResponse::ok);
    }
}
