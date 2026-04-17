package net.tylersoft.users.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.LookupRequest;
import net.tylersoft.users.common.ApiResponse;
import net.tylersoft.users.dto.*;
import net.tylersoft.users.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
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
     * <p>Accepts {@code multipart/form-data} with three parts:
     * <ul>
     *   <li>{@code data} — JSON object (Content-Type: application/json) with customer
     *       details and ID metadata: firstName, lastName, phoneNumber, email,
     *       idType (NATIONAL_ID|PASSPORT|DRIVING_LICENSE), idNumber (optional)</li>
     *   <li>{@code id_front} — front-side image of the identity document</li>
     *   <li>{@code id_back} — back-side image (optional for passports)</li>
     * </ul>
     *
     * <p>On success the customer is created in {@code INITIATED} status, document
     * images are stored, and an OTP is dispatched to the supplied phone number.
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<CustomerResponse>> register(
            @RequestPart("data") RegisterRequest request,
            ServerWebExchange serverWebExchange) {

        return validate(request)
                .then(customerService.register(request, serverWebExchange))
                .map(r -> ApiResponse.ok("Registration successful. OTP sent to " + request.phoneNumber(), r));
    }


    /**
     * Step 2 — Verify the OTP sent to the customer's phone.
     * On success the customer advances to {@code PHONE_VERIFIED}.
     */
    @PostMapping("/verify-otp")
    public Mono<ApiResponse<CustomerResponse>> verifyOtp(
            @Validated @RequestBody VerifyOtpRequest request) {
        return customerService.verifyOtp(request)
                .map(r -> ApiResponse.ok("Phone number verified successfully", r));
    }

    /**
     * Resend an OTP for the given phone number and purpose.
     * Useful when the original OTP expired or was not received.
     */
    @PostMapping("/resend-otp")
    public Mono<ApiResponse<Void>> resendOtp(
            @Validated @RequestBody ResendOtpRequest request) {
        return customerService.resendOtp(request)
                .thenReturn(ApiResponse.<Void>ok("OTP resent successfully", null));
    }


    @PostMapping("/{customerId}/set-pin")
    public Mono<ApiResponse<CustomerResponse>> setPin(
            @PathVariable UUID customerId,
            @Validated @RequestBody SetPinRequest request) {
        return customerService.setPin(customerId, request)
                .map(r -> ApiResponse.ok("PIN set successfully", r));
    }

    // ── Profile ───────────────────────────────────────────────────────────────

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


    /**
     * Programmatically validates any bean against its constraint annotations.
     * Used for multipart endpoints where {@code @Validated @RequestBody} is not applicable.
     * Emits an {@link IllegalArgumentException} with all violations joined if any fail.
     */
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
