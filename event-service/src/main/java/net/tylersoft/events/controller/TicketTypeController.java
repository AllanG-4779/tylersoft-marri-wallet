package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.events.dto.IdRequest;
import net.tylersoft.events.dto.tickettype.*;
import net.tylersoft.events.service.TicketTypeService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ticket-types")
@RequiredArgsConstructor
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    @PostMapping("/list")
    public Mono<ApiResponse<List<TicketTypeResponse>>> list(@RequestBody @Valid ListTicketTypesRequest req) {
        return ticketTypeService.listByEvent(req.eventId()).collectList().map(ApiResponse::ok);
    }

    @PostMapping("/get")
    public Mono<ApiResponse<TicketTypeResponse>> get(@RequestBody @Valid IdRequest req) {
        return ticketTypeService.getById(req.id()).map(ApiResponse::ok);
    }

    @PostMapping("/create")
    public Mono<ApiResponse<TicketTypeResponse>> create(@RequestBody @Valid CreateTicketTypeRequest req) {
        return ticketTypeService.create(req.eventId(), req).map(ApiResponse::ok);
    }

    @PostMapping("/update")
    public Mono<ApiResponse<TicketTypeResponse>> update(@RequestBody @Valid UpdateTicketTypeRequest req) {
        return ticketTypeService.update(req.id(), req).map(ApiResponse::ok);
    }

    @PostMapping("/delete")
    public Mono<ApiResponse<Void>> delete(@RequestBody @Valid DeleteTicketTypeRequest req) {
        return ticketTypeService.delete(req.eventId(), req.id()).thenReturn(ApiResponse.<Void>ok(null));
    }
}
