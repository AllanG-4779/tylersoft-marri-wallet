package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.events.dto.planner.AddPlannerRequest;
import net.tylersoft.events.dto.planner.PlannerResponse;
import net.tylersoft.events.service.EventPlannerService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/planners")
@RequiredArgsConstructor
public class EventPlannerController {

    private final EventPlannerService plannerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MERCHANT')")
    public Mono<ApiResponse<PlannerResponse>> add(
            @PathVariable UUID eventId,
            @Valid @RequestBody UniversalRequestWrapper<AddPlannerRequest> body) {
        return plannerService.add(eventId, body.data())
                .map(r -> ApiResponse.ok("Planner added", r));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MERCHANT')")
    public Mono<ApiResponse<List<PlannerResponse>>> list(@PathVariable UUID eventId) {
        return plannerService.listByEvent(eventId).collectList().map(ApiResponse::ok);
    }

    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MERCHANT')")
    public Mono<ApiResponse<String>> remove(
            @PathVariable UUID eventId,
            @PathVariable UUID customerId) {
        return plannerService.remove(eventId, customerId)
                .flatMap(each -> Mono.just(ApiResponse.ok("Planner removed")));
    }
}
