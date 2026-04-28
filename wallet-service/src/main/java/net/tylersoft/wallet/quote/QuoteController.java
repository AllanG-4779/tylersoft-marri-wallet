package net.tylersoft.wallet.quote;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.wallet.config.CustomerOnly;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/transactions")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @CustomerOnly
    @PostMapping("/enquire")
    public Mono<ApiResponse<QuoteResponse>> enquire(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UniversalRequestWrapper<TransactionEnquiryRequest> body) {
        return quoteService.enquire(jwt, body.data())
                .map(ApiResponse::ok)
                .onErrorResume(ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }

    @CustomerOnly
    @PostMapping("/confirm")
    public Mono<ApiResponse<ConfirmResponse>> confirm(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UniversalRequestWrapper<ConfirmRequest> body) {
        return quoteService.confirm(jwt, body.data())
                .map(ApiResponse::ok)
                .onErrorResume(ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }
}
