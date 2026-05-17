package net.tylersoft.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.events.dto.promocode.*;
import net.tylersoft.events.service.PromoCodeService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promo-codes")
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @PostMapping("/list")
    public Mono<ApiResponse<List<PromoCodeResponse>>> list(@RequestBody @Valid ListPromoCodesRequest req) {
        return promoCodeService.listByEvent(req.eventId()).collectList().map(ApiResponse::ok);
    }

    @PostMapping("/get")
    public Mono<ApiResponse<PromoCodeResponse>> get(@RequestBody @Valid PromoCodeRefRequest req) {
        return promoCodeService.getById(req.eventId(), req.id()).map(ApiResponse::ok);
    }

    @PostMapping("/create")
    public Mono<ApiResponse<PromoCodeResponse>> create(@RequestBody @Valid CreatePromoCodeRequest req) {
        return promoCodeService.create(req.eventId(), req).map(ApiResponse::ok);
    }

    @PostMapping("/update")
    public Mono<ApiResponse<PromoCodeResponse>> update(@RequestBody @Valid UpdatePromoCodeRequest req) {
        return promoCodeService.update(req.eventId(), req.id(), req).map(ApiResponse::ok);
    }

    @PostMapping("/delete")
    public Mono<ApiResponse<Void>> delete(@RequestBody @Valid PromoCodeRefRequest req) {
        return promoCodeService.delete(req.eventId(), req.id()).thenReturn(ApiResponse.<Void>ok(null));
    }
}
