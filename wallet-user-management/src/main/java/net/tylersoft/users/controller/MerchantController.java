package net.tylersoft.users.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.users.dto.merchant.MerchantQrRequest;
import net.tylersoft.users.dto.merchant.MerchantQrResponse;
import net.tylersoft.users.dto.merchant.MerchantRegistrationRequest;
import net.tylersoft.users.dto.merchant.MerchantResponse;
import net.tylersoft.users.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final Validator validator;

    /**
     * Self-registration. Accepts multipart/form-data.
     * The {@code data} part is JSON with business details.
     * Any additional file parts (business_reg, tax_cert, etc.) are stored as documents.
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<MerchantResponse>> register(
            @RequestPart("data") UniversalRequestWrapper<MerchantRegistrationRequest> request,
            ServerWebExchange exchange) {
        return validate(request.data())
                .then(merchantService.register(request.data(), exchange))
                .map(r -> ApiResponse.ok("Merchant registration submitted for review", r));
    }

    /** Lookup a merchant by merchant code — called by the customer app after a QR scan. */
    @GetMapping("/lookup/{merchantCode}")
    public Mono<ApiResponse<MerchantResponse>> lookupByCode(@PathVariable String merchantCode) {
        return merchantService.getByMerchantCode(merchantCode)
                .map(ApiResponse::ok);
    }

    @GetMapping("/{merchantId}")
    public Mono<ApiResponse<MerchantResponse>> getById(@PathVariable UUID merchantId) {
        return merchantService.getById(merchantId)
                .map(ApiResponse::ok);
    }

    /** Generate the merchant's QR code on-demand (e.g. for in-app display). */
    @GetMapping("/{merchantId}/qr")
    public Mono<ApiResponse<MerchantQrResponse>> getQr(@PathVariable UUID merchantId) {
        return merchantService.generateQr(merchantId, null)
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
