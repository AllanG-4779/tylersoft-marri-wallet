package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.events.dto.validation.ValidateTicketRequest;
import net.tylersoft.events.dto.validation.ValidationResponse;
import net.tylersoft.events.service.TicketValidationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}")
@RequiredArgsConstructor
public class TicketValidationController {

    private final TicketValidationService validationService;

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MERCHANT')")
    public Mono<ApiResponse<ValidationResponse>> validate(
            @PathVariable UUID eventId,
            @Valid @RequestBody UniversalRequestWrapper<ValidateTicketRequest> body,
            @AuthenticationPrincipal Jwt jwt) {
        String validatedBy = jwt.getClaimAsString("username");
        return validationService.validate(body.data().ticketCode(), eventId, validatedBy)
                .map(r -> ApiResponse.ok(r.message(), r));
    }

    @GetMapping("/validations")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MERCHANT')")
    public Mono<ApiResponse<Page<ValidationResponse>>> listValidations(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return validationService.getValidations(eventId, page, size).map(ApiResponse::ok);
    }
}
