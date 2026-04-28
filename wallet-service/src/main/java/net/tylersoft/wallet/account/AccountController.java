package net.tylersoft.wallet.account;

import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountOpeningService accountOpeningService;
    private final AccountQueryService   accountQueryService;

    @PostMapping
    public Mono<ApiResponse<OpenAccountResult>> openAccount(
            @RequestBody UniversalRequestWrapper<OpenAccountRequest> request) {
        OpenAccountRequest data = request.data();
        return accountOpeningService.openAccount(
                        data.currency(),
                        data.accountPrefix(),
                        data.phoneNumber(),
                        data.accountName(),
                        "create", "")
                .map(result -> result.isSuccess()
                        ? ApiResponse.ok(result)
                        : ApiResponse.error(result.statusCode() + " - " + result.message()));
    }

    @PostMapping("/enquiry")
    public Mono<ApiResponse<EnquiryResponse>> enquiry(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UniversalRequestWrapper<AccountEnquiryRequest> request) {
        return accountQueryService.enquire(jwt, request.data())
                .map(ApiResponse::ok)
                .onErrorResume(ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }
}
