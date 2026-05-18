package net.tylersoft.wallet.account;

import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v2/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountOpeningService accountOpeningService;
    private final AccountQueryService   accountQueryService;
     @PreAuthorize("hasRole('SERVICE') OR hasRole('INTEGRATOR')")
    @PostMapping
    public Mono<ApiResponse<OpenAccountResult>> openAccount(
            @RequestBody UniversalRequestWrapper<OpenAccountRequest> request) {
        OpenAccountRequest data = request.data();

        return accountOpeningService.openAccount(
                        data.currency(),
                        data.accountPrefix()==null?"TA":data.accountPrefix(),
                        data.phoneNumber(),
                        data.accountName(),
                        "create", "")
                .map(result -> result.isSuccess()
                        ? ApiResponse.ok(result)
                        : ApiResponse.error(result.statusCode() + " - " + result.message()));
    }

    @GetMapping("/by-phone/{phoneNumber}")
    public Mono<ApiResponse<List<AccountSummary>>> getByPhone(@PathVariable String phoneNumber) {
        return accountQueryService.getAccountsByPhone(phoneNumber)
                .collectList()
                .map(ApiResponse::ok);
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
