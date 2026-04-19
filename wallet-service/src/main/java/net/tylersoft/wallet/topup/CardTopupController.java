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

    @PostMapping("/initiate")
    public Mono<ApiResponse<CardTopupInitiateResponse>> initiate(
            @RequestBody UniversalRequestWrapper<CardTopupRequest> request) {
        return cardTopupService.initiate(request.data())
                .map(ApiResponse::ok)
                .onErrorResume(IllegalArgumentException.class,
                        ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }

    @PostMapping("/callback")
    public Mono<ApiResponse<Void>> callback(
            @RequestBody UniversalRequestWrapper<CardTopupCallbackRequest> request) {
        return cardTopupService.handleCallback(request.data())
                .thenReturn(ApiResponse.<Void>ok(null))
                .onErrorResume(IllegalArgumentException.class,
                        ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }
}
