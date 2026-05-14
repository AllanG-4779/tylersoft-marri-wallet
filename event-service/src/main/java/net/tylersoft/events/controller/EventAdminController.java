package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.events.dto.event.CreateEventRequest;
import net.tylersoft.events.dto.event.EventResponse;
import net.tylersoft.events.dto.event.EventStatusUpdateRequest;
import net.tylersoft.events.dto.event.UpdateEventRequest;
import net.tylersoft.events.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
public class EventAdminController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<EventResponse>> create(
            @Valid @RequestBody UniversalRequestWrapper<CreateEventRequest> body,
            @AuthenticationPrincipal Jwt jwt) {
        return eventService.create(body.data(), jwt.getClaimAsString("username"))
                .map(r -> ApiResponse.ok("Event created", r));
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<Page<EventResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = status != null
                ? eventService.listByStatus(status, page, size)
                : eventService.listAll(page, size);
        return result.map(ApiResponse::ok);
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<EventResponse>> getById(@PathVariable UUID eventId) {
        return eventService.getById(eventId).map(ApiResponse::ok);
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<EventResponse>> update(
            @PathVariable UUID eventId,
            @RequestBody UniversalRequestWrapper<UpdateEventRequest> body) {
        return eventService.update(eventId, body.data())
                .map(r -> ApiResponse.ok("Event updated", r));
    }

    @PostMapping("/{eventId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Mono<ApiResponse<EventResponse>> updateStatus(
            @PathVariable UUID eventId,
            @Valid @RequestBody UniversalRequestWrapper<EventStatusUpdateRequest> body,
            @AuthenticationPrincipal Jwt jwt) {
        EventStatusUpdateRequest req = body.data();
        return eventService.updateStatus(eventId, req.action(), req.reason(), jwt.getClaimAsString("username"))
                .map(r -> ApiResponse.ok("Event status updated to " + r.status(), r));
    }
}
