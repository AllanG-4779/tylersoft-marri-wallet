package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.events.dto.purchase.CreatePurchaseRequest;
import net.tylersoft.events.dto.purchase.EventTicketResponse;
import net.tylersoft.events.dto.purchase.PurchaseResponse;
import net.tylersoft.events.service.TicketPurchaseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/purchases")
@RequiredArgsConstructor
public class TicketPurchaseController {

    private final TicketPurchaseService purchaseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<PurchaseResponse>> initiate(
            @Valid @RequestBody UniversalRequestWrapper<CreatePurchaseRequest> body,
            @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return purchaseService.initiate(customerId, body.data())
                .map(r -> ApiResponse.ok("Purchase initiated", r));
    }

    @GetMapping
    public Mono<ApiResponse<Page<PurchaseResponse>>> myPurchases(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return purchaseService.getMyPurchases(customerId, page, size).map(ApiResponse::ok);
    }

    @GetMapping("/{purchaseId}")
    public Mono<ApiResponse<PurchaseResponse>> getPurchase(
            @PathVariable UUID purchaseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return purchaseService.getPurchase(purchaseId, customerId).map(ApiResponse::ok);
    }

    @PostMapping("/{purchaseId}/cancel")
    public Mono<ApiResponse<PurchaseResponse>> cancel(
            @PathVariable UUID purchaseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return purchaseService.cancel(purchaseId, customerId)
                .map(r -> ApiResponse.ok("Purchase cancelled", r));
    }

    @GetMapping("/tickets")
    public Mono<ApiResponse<List<EventTicketResponse>>> myTickets(@AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return purchaseService.getMyTickets(customerId).collectList().map(ApiResponse::ok);
    }

    // ── Admin: confirm purchase after payment ─────────────────────────────────

    @PostMapping("/{purchaseId}/confirm")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<PurchaseResponse>> confirm(
            @PathVariable UUID purchaseId,
            @RequestBody UniversalRequestWrapper<ConfirmPurchaseRequest> body) {
        return purchaseService.confirm(purchaseId, body.data().paymentReference())
                .map(r -> ApiResponse.ok("Purchase confirmed and tickets issued", r));
    }

    record ConfirmPurchaseRequest(String paymentReference) {}
}
