package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.events.dto.ticketclass.CreateTicketClassRequest;
import net.tylersoft.events.dto.ticketclass.TicketClassResponse;
import net.tylersoft.events.dto.ticketclass.UpdateTicketClassRequest;
import net.tylersoft.events.service.EventTicketClassService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/ticket-classes")
@RequiredArgsConstructor
public class TicketClassController {

    private final EventTicketClassService ticketClassService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MERCHANT')")
    public Mono<ApiResponse<TicketClassResponse>> add(
            @PathVariable UUID eventId,
            @Valid @RequestBody UniversalRequestWrapper<CreateTicketClassRequest> body) {
        return ticketClassService.add(eventId, body.data())
                .map(r -> ApiResponse.ok("Ticket class added", r));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MERCHANT')")
    public Mono<ApiResponse<List<TicketClassResponse>>> list(@PathVariable UUID eventId) {
        return ticketClassService.listByEvent(eventId).collectList().map(ApiResponse::ok);
    }

    @PutMapping("/{classId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MERCHANT')")
    public Mono<ApiResponse<TicketClassResponse>> update(
            @PathVariable UUID eventId,
            @PathVariable UUID classId,
            @RequestBody UniversalRequestWrapper<UpdateTicketClassRequest> body) {
        return ticketClassService.update(classId, body.data())
                .map(r -> ApiResponse.ok("Ticket class updated", r));
    }

    @DeleteMapping("/{classId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MERCHANT')")
    public Mono<ApiResponse<TicketClassResponse>> deactivate(
            @PathVariable UUID eventId,
            @PathVariable UUID classId) {
        return ticketClassService.deactivate(classId)
                .map(r -> ApiResponse.ok("Ticket class deactivated", r));
    }
}
