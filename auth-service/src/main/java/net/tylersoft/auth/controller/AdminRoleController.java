package net.tylersoft.auth.controller;

import lombok.RequiredArgsConstructor;
import net.tylersoft.auth.dto.AdminRoleRequest;
import net.tylersoft.auth.dto.AdminRoleResponse;
import net.tylersoft.auth.service.AdminRoleService;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<AdminRoleResponse>> create(
            @RequestBody UniversalRequestWrapper<AdminRoleRequest> request) {
        return adminRoleService.create(request.data())
                .map(r -> ApiResponse.ok("Role created", r));
    }

    @GetMapping
    public Mono<ApiResponse<List<AdminRoleResponse>>> listAll() {
        return adminRoleService.listAll()
                .collectList()
                .map(ApiResponse::ok);
    }

    @DeleteMapping("/{roleId}")
    public Mono<ApiResponse<Void>> delete(@PathVariable UUID roleId) {
        return adminRoleService.delete(roleId)
                .thenReturn(ApiResponse.ok("Role deleted", null));
    }
}
