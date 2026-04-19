package net.tylersoft.wallet.account;

import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.common.http.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountOpeningService accountOpeningService;

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
}