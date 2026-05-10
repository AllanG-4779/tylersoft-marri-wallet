package net.tylersoft.auth.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import net.tylersoft.auth.dto.CreateSystemAdminRequest;
import net.tylersoft.auth.dto.SystemAdminCredentialResponse;
import net.tylersoft.auth.dto.SystemAdminResponse;
import net.tylersoft.auth.service.AdminRoleService;
import net.tylersoft.auth.service.SystemAdminService;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/system-admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemAdminController {

    private final SystemAdminService systemAdminService;
    private final AdminRoleService adminRoleService;
    private final Validator validator;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<SystemAdminCredentialResponse>> create(
            @RequestBody UniversalRequestWrapper<CreateSystemAdminRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        String createdBy = jwt.getClaimAsString("username");
        return validate(request.data())
                .then(systemAdminService.create(request.data(), createdBy))
                .map(r -> ApiResponse.ok("Admin account created. Share the temporary password securely.", r));
    }

    @GetMapping
    public Mono<ApiResponse<Page<SystemAdminResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return systemAdminService.list(page, size).map(ApiResponse::ok);
    }

    @GetMapping("/{adminId}")
    public Mono<ApiResponse<SystemAdminResponse>> getById(@PathVariable UUID adminId) {
        return systemAdminService.getById(adminId).map(ApiResponse::ok);
    }

    @PatchMapping("/{adminId}/enable")
    public Mono<ApiResponse<SystemAdminResponse>> enable(@PathVariable UUID adminId) {
        return systemAdminService.enable(adminId)
                .map(r -> ApiResponse.ok("Admin account enabled", r));
    }

    @PatchMapping("/{adminId}/disable")
    public Mono<ApiResponse<SystemAdminResponse>> disable(@PathVariable UUID adminId) {
        return systemAdminService.disable(adminId)
                .map(r -> ApiResponse.ok("Admin account disabled", r));
    }

    @PostMapping("/{adminId}/reset-password")
    public Mono<ApiResponse<SystemAdminCredentialResponse>> resetPassword(@PathVariable UUID adminId) {
        return systemAdminService.resetPassword(adminId)
                .map(r -> ApiResponse.ok("Password reset. Share the temporary password securely.", r));
    }

    @PostMapping("/{adminId}/roles/{roleId}")
    public Mono<ApiResponse<Void>> assignRole(@PathVariable UUID adminId, @PathVariable UUID roleId) {
        return adminRoleService.assign(adminId, roleId)
                .thenReturn(ApiResponse.ok("Role assigned", null));
    }

    @DeleteMapping("/{adminId}/roles/{roleId}")
    public Mono<ApiResponse<Void>> removeRole(@PathVariable UUID adminId, @PathVariable UUID roleId) {
        return adminRoleService.remove(adminId, roleId)
                .thenReturn(ApiResponse.ok("Role removed from admin", null));
    }

    private <T> Mono<Void> validate(T target) {
        Set<ConstraintViolation<T>> violations = validator.validate(target);
        if (violations.isEmpty()) return Mono.empty();
        String message = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        return Mono.error(new IllegalArgumentException(message));
    }
}
