package net.tylersoft.users.controller;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.users.dto.merchant.MerchantQrResponse;
import net.tylersoft.users.dto.merchant.MerchantResponse;
import net.tylersoft.users.service.MerchantService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /** Lookup a merchant by merchant code — called by the customer app after a QR scan. */
    @GetMapping("/lookup/{merchantCode}")
    public Mono<ApiResponse<MerchantResponse>> lookupByCode(@PathVariable String merchantCode) {
        return merchantService.getByMerchantCode(merchantCode)
                .map(ApiResponse::ok);
    }

    @GetMapping("/{merchantId}")
    public Mono<ApiResponse<MerchantResponse>> getById(@PathVariable UUID merchantId) {
        return merchantService.getById(merchantId)
                .map(ApiResponse::ok);
    }

    /** Generate the merchant's QR code on-demand (e.g. for in-app display). */
    @GetMapping("/{merchantId}/qr")
    public Mono<ApiResponse<MerchantQrResponse>> getQr(@PathVariable UUID merchantId) {
        return merchantService.generateQr(merchantId, null)
                .map(ApiResponse::ok);
    }
}
