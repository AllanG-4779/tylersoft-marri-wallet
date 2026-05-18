package net.tylersoft.wallet.transfer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.wallet.config.IntegratorOnly;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/integrator/transfer")
@RequiredArgsConstructor
public class IntegratorTransferController {

    private final IntegratorTransferService transferService;

    @IntegratorOnly
    @PostMapping("/ft")
    public Mono<ApiResponse<FTResponse>> fundTransfer(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid IntegratorFTRequest req) {
        return transferService.transfer(req, jwt.getSubject())
                .map(result -> "00".equals(result.responseCode())
                        ? ApiResponse.ok(result)
                        : ApiResponse.error(result.responseMessage(), result.responseCode(), result))
                .onErrorResume(ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }
}
