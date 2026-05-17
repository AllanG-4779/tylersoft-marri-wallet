package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.events.dto.IdRequest;
import net.tylersoft.events.dto.pricingtier.*;
import net.tylersoft.events.service.PricingTierService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing-tiers")
@RequiredArgsConstructor
public class PricingTierController {

    private final PricingTierService pricingTierService;

    @PostMapping("/list")
    public Mono<ApiResponse<List<PricingTierResponse>>> list(@RequestBody @Valid ListPricingTiersRequest req) {
        return pricingTierService.listByTicketType(req.ticketTypeId()).collectList().map(ApiResponse::ok);
    }

    @PostMapping("/get")
    public Mono<ApiResponse<PricingTierResponse>> get(@RequestBody @Valid IdRequest req) {
        return pricingTierService.getById(req.id()).map(ApiResponse::ok);
    }

    @PostMapping("/create")
    public Mono<ApiResponse<PricingTierResponse>> create(@RequestBody @Valid CreatePricingTierRequest req) {
        return pricingTierService.create(req.ticketTypeId(), req).map(ApiResponse::ok);
    }

    @PostMapping("/update")
    public Mono<ApiResponse<PricingTierResponse>> update(@RequestBody @Valid UpdatePricingTierRequest req) {
        return pricingTierService.update(req.id(), req).map(ApiResponse::ok);
    }

    @PostMapping("/delete")
    public Mono<ApiResponse<Void>> delete(@RequestBody @Valid DeletePricingTierRequest req) {
        return pricingTierService.delete(req.ticketTypeId(), req.id()).thenReturn(ApiResponse.<Void>ok(null));
    }
}
