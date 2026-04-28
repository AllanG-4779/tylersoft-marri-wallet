package net.tylersoft.users.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.LookupRequest;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.users.dto.*;
import net.tylersoft.users.service.CustomerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class UserController {

    private final CustomerService customerService;
    private final Validator validator;

    /**
     * Step 1 — Register a new customer.
     *
     * Accepts {@code multipart/form-data} with three parts:
     * - {@code data} — JSON with customer details + device context (deviceId, deviceType, channel)
     * - {@code id_front} — front-side image of the identity document
     * - {@code id_back} — back-side image (optional for passports)
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<CustomerResponse>> register(
            @RequestPart("data") UniversalRequestWrapper<RegisterRequest> request,
            ServerWebExchange serverWebExchange) {

        return validate(request)
                .then(customerService.register(request.data(), serverWebExchange))
                .map(r -> ApiResponse.ok("Registration successful. OTP sent to " + request.data().phoneNumber(), r));
    }

    /** Step 2 — Verify the OTP sent to the customer's phone. */
    @PostMapping("/verify-otp")
    public Mono<ApiResponse<CustomerResponse>> verifyOtp(
            @Validated @RequestBody UniversalRequestWrapper<VerifyOtpRequest> request) {
        return customerService.verifyOtp(request.data())
                .map(r -> ApiResponse.ok("Phone number verified successfully", r));
    }

    /** Resend OTP for the given phone number and purpose. */
    @PostMapping("/resend-otp")
    public Mono<ApiResponse<Void>> resendOtp(
            @Validated @RequestBody UniversalRequestWrapper<ResendOtpRequest> request) {
        return customerService.resendOtp(request.data())
                .thenReturn(ApiResponse.<Void>ok("OTP resent successfully", null));
    }

    @PostMapping("/set-pin")
    public Mono<ApiResponse<CustomerResponse>> setPin(

            @Validated @RequestBody UniversalRequestWrapper<SetPinRequest> request) {
        return customerService.setPin(request)
                .map(r -> ApiResponse.ok("PIN set successfully", r));
    }

    @GetMapping("/{customerId}")
    public Mono<ApiResponse<CustomerResponse>> getProfile(@PathVariable UUID customerId) {
        return customerService.getProfile(customerId)
                .map(ApiResponse::ok);
    }

    @PostMapping("/lookup")
    public Mono<ApiResponse<CustomerResponse>> lookupByPhone(
            @RequestBody LookupRequest request) {
        return customerService.lookupByPhoneNumber(request.phoneNumber())
                .map(ApiResponse::ok);
    }

    @GetMapping("/{customerId}/kyc/documents")
    public Mono<ApiResponse<List<DocumentResponse>>> getDocuments(@PathVariable UUID customerId) {
        return customerService.getDocuments(customerId)
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
