package net.tylersoft.users.controller;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.users.dto.merchant.*;
import net.tylersoft.users.model.MerchantDocument;
import net.tylersoft.users.service.MerchantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
public class MerchantAdminController {

    private final MerchantService merchantService;

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<List<MerchantResponse>>> listAll(
            @RequestParam(required = false) String status) {
        var flux = status != null
                ? merchantService.listByStatus(status)
                : merchantService.listAll();
        return flux.collectList().map(ApiResponse::ok);
    }

    @GetMapping("/{merchantId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<MerchantResponse>> getById(@PathVariable UUID merchantId) {
        return merchantService.getById(merchantId).map(ApiResponse::ok);
    }

    @PostMapping("/{merchantId}/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<MerchantResponse>> approve(
            @PathVariable UUID merchantId,
            @AuthenticationPrincipal Jwt jwt) {
        String admin = jwt.getClaimAsString("username");
        return merchantService.approve(merchantId, admin)
                .map(r -> ApiResponse.ok("Merchant approved and MA account created", r));
    }

    @PostMapping("/{merchantId}/reject")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<MerchantResponse>> reject(
            @PathVariable UUID merchantId,
            @RequestBody UniversalRequestWrapper<MerchantStatusUpdateRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        String admin = jwt.getClaimAsString("username");
        return merchantService.reject(merchantId, request.data().reason(), admin)
                .map(r -> ApiResponse.ok("Merchant rejected", r));
    }

    @PostMapping("/{merchantId}/suspend")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<MerchantResponse>> suspend(
            @PathVariable UUID merchantId,
            @RequestBody UniversalRequestWrapper<MerchantStatusUpdateRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        String admin = jwt.getClaimAsString("username");
        return merchantService.suspend(merchantId, request.data().reason(), admin)
                .map(r -> ApiResponse.ok("Merchant suspended", r));
    }

    @PostMapping("/{merchantId}/reactivate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<MerchantResponse>> reactivate(
            @PathVariable UUID merchantId,
            @AuthenticationPrincipal Jwt jwt) {
        String admin = jwt.getClaimAsString("username");
        return merchantService.reactivate(merchantId, admin)
                .map(r -> ApiResponse.ok("Merchant reactivated", r));
    }

    /** Generate a QR code dynamically for a merchant. */
    @PostMapping("/{merchantId}/qr")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<MerchantQrResponse>> generateQr(
            @PathVariable UUID merchantId,
            @RequestBody(required = false) UniversalRequestWrapper<MerchantQrRequest> request) {
        MerchantQrRequest qrReq = request != null ? request.data() : null;
        return merchantService.generateQr(merchantId, qrReq)
                .map(r -> ApiResponse.ok("QR code generated", r));
    }

    @GetMapping("/{merchantId}/documents")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<List<MerchantDocument>>> getDocuments(@PathVariable UUID merchantId) {
        return merchantService.getDocuments(merchantId)
                .collectList()
                .map(ApiResponse::ok);
    }
}
