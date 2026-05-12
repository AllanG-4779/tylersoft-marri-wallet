package net.tylersoft.users.controller;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.users.dto.AdminCustomerDetailResponse;
import net.tylersoft.users.dto.CustomerResponse;
import net.tylersoft.users.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<Page<CustomerResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return customerService.adminList(status, page, size).map(ApiResponse::ok);
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<AdminCustomerDetailResponse>> getDetail(@PathVariable UUID customerId) {
        return customerService.adminDetail(customerId).map(ApiResponse::ok);
    }
}
