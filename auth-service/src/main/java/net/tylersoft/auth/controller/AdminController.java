package net.tylersoft.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.auth.dto.AdminLoginRequest;
import net.tylersoft.auth.dto.CreateIntegratorRequest;
import net.tylersoft.auth.dto.CreateIntegratorResponse;
import net.tylersoft.auth.dto.LoginResponse;
import net.tylersoft.auth.service.AdminAuthService;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/admin/login")
    public Mono<ApiResponse<LoginResponse>> adminLogin(
             @RequestBody UniversalRequestWrapper<AdminLoginRequest> request) {
        return adminAuthService.login(request.data())
                .map(ApiResponse::ok);
    }

    @PostMapping("/integrator")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<CreateIntegratorResponse>> createIntegrator(
            @Validated @RequestBody UniversalRequestWrapper<CreateIntegratorRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        String createdBy = jwt.getClaimAsString("username");
        return adminAuthService.createIntegrator(request.data(), createdBy)
                .map(ApiResponse::ok);
    }
}
