package net.tylersoft.users.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.users.dto.merchant.*;
import net.tylersoft.users.model.MerchantDocument;
import net.tylersoft.users.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
public class MerchantAdminController {

    private final MerchantService merchantService;
    private final Validator validator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
//   
    public Mono<ApiResponse<MerchantResponse>> create(
            @RequestPart("data") UniversalRequestWrapper<MerchantRegistrationRequest> request,
            @AuthenticationPrincipal Jwt jwt,
            ServerWebExchange exchange) {
        String admin = jwt.getClaimAsString("username");
        return validate(request.data())
                .then(merchantService.register(request.data(), admin, exchange))
                .map(r -> ApiResponse.ok("Merchant created", r));
    }

    @GetMapping
   
    public Mono<ApiResponse<Page<MerchantResponse>>> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = status != null
                ? merchantService.listByStatus(status, page, size)
                : merchantService.listAll(page, size);
        return result.map(ApiResponse::ok);
    }

    @GetMapping("/{merchantId}")
   
    public Mono<ApiResponse<MerchantResponse>> getById(@PathVariable UUID merchantId) {
        return merchantService.getById(merchantId).map(ApiResponse::ok);
    }

    @PostMapping("/{merchantId}/approve")
   
    public Mono<ApiResponse<MerchantResponse>> approve(
            @PathVariable UUID merchantId,
            @AuthenticationPrincipal Jwt jwt) {
        String admin = jwt.getClaimAsString("username");
        return merchantService.approve(merchantId, admin)
                .map(r -> ApiResponse.ok("Merchant approved and MA account created", r));
    }

    @PostMapping("/{merchantId}/reject")
   
    public Mono<ApiResponse<MerchantResponse>> reject(
            @PathVariable UUID merchantId,
            @RequestBody UniversalRequestWrapper<MerchantStatusUpdateRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        String admin = jwt.getClaimAsString("username");
        return merchantService.reject(merchantId, request.data().reason(), admin)
                .map(r -> ApiResponse.ok("Merchant rejected", r));
    }

    @PostMapping("/{merchantId}/suspend")
   
    public Mono<ApiResponse<MerchantResponse>> suspend(
            @PathVariable UUID merchantId,
            @RequestBody UniversalRequestWrapper<MerchantStatusUpdateRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        String admin = jwt.getClaimAsString("username");
        return merchantService.suspend(merchantId, request.data().reason(), admin)
                .map(r -> ApiResponse.ok("Merchant suspended", r));
    }

    @PostMapping("/{merchantId}/reactivate")
   
    public Mono<ApiResponse<MerchantResponse>> reactivate(
            @PathVariable UUID merchantId,
            @AuthenticationPrincipal Jwt jwt) {
        String admin = jwt.getClaimAsString("username");
        return merchantService.reactivate(merchantId, admin)
                .map(r -> ApiResponse.ok("Merchant reactivated", r));
    }

    /** Generate a QR code dynamically for a merchant. */
    @PostMapping("/{merchantId}/qr")
   
    public Mono<ApiResponse<MerchantQrResponse>> generateQr(
            @PathVariable UUID merchantId,
            @RequestBody(required = false) UniversalRequestWrapper<MerchantQrRequest> request) {
        MerchantQrRequest qrReq = request != null ? request.data() : null;
        return merchantService.generateQr(merchantId, qrReq)
                .map(r -> ApiResponse.ok("QR code generated", r));
    }

    @GetMapping("/{merchantId}/documents")
   
    public Mono<ApiResponse<List<MerchantDocument>>> getDocuments(@PathVariable UUID merchantId) {
        return merchantService.getDocuments(merchantId)
                .collectList()
                .map(ApiResponse::ok);
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
