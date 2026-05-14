package net.tylersoft.events.controller;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.events.dto.event.EventResponse;
import net.tylersoft.events.service.EventService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public Mono<ApiResponse<EventResponse>> getById(@PathVariable UUID eventId) {
        return eventService.getById(eventId).map(ApiResponse::ok);
    }

    @GetMapping("/merchant/{merchantCode}")
    public Mono<ApiResponse<Page<EventResponse>>> listByMerchant(
            @PathVariable String merchantCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return eventService.listByMerchant(merchantCode, page, size).map(ApiResponse::ok);
    }
}
