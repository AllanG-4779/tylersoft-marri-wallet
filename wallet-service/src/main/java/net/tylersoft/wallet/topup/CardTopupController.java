package net.tylersoft.wallet.topup;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/topup/card")
@RequiredArgsConstructor
public class CardTopupController {

    private final CardTopupService cardTopupService;

    /**
     * Phase 1 — device profiling.
     * Client supplies card + account details; wallet-service calls the payment gateway
     * device fingerprint endpoint and returns the 3DS collection URL + esbRef for Phase 2.
     */
    @PostMapping("/profile")
    public Mono<ApiResponse<CardProfileResponse>> profile(
            @RequestBody UniversalRequestWrapper<CardProfileRequest> request) {
        return cardTopupService.profile(request.data())
                .map(ApiResponse::ok)
                .onErrorResume(IllegalArgumentException.class,
                        ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }

    /**
     * Phase 2 — charge initiation.
     * Client supplies the esbRef from Phase 1, card details, and browser/device data
     * captured during 3DS collection. Wallet-service charges the card via the payment gateway.
     */
    @PostMapping("/initiate")
    public Mono<ApiResponse<CardTopupInitiateResponse>> initiate(
            @RequestBody UniversalRequestWrapper<CardTopupPaymentRequest> request) {
        return cardTopupService.initiate(request.data())
                .map(ApiResponse::ok)
                .onErrorResume(ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }

    /**
     * Phase 3 — payment gateway callback.
     * Called by the payment gateway (or payment-service) when the card charge completes.
     */
    @PostMapping("/callback")
    public Mono<ApiResponse<Void>> callback(
            @RequestBody UniversalRequestWrapper<CardTopupCallbackRequest> request) {
        return cardTopupService.handleCallback(request.data())
                .thenReturn(ApiResponse.<Void>ok(null))
                .onErrorResume(IllegalArgumentException.class,
                        ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }
}
