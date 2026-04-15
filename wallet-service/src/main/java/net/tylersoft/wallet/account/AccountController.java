package net.tylersoft.wallet.account;

import net.tylersoft.wallet.common.ApiResponse;
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
            @RequestBody OpenAccountRequest request) {

        return accountOpeningService.openAccount(
                        request.currency(),
                        request.accountPrefix(),
                        request.phoneNumber(),
                        request.accountName(),
                        "create", "")
                .map(result -> result.isSuccess()
                        ? ApiResponse.ok(result)
                        : ApiResponse.error(result.statusCode() + " - " + result.message()));
    }
}