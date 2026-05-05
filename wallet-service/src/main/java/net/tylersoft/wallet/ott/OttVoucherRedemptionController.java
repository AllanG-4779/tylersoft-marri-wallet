package net.tylersoft.wallet.ott;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.wallet.config.CustomerOnly;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ott")
@RequiredArgsConstructor
public class OttVoucherRedemptionController {

    private final OttVoucherRedemptionService redemptionService;

    @CustomerOnly
    @PostMapping("/redeem")
    public Mono<ApiResponse<OttRedeemResponse>> redeemVoucher(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UniversalRequestWrapper<OttRedeemRequest> body) {
        String phoneNumber = jwt.getClaimAsString("phone");
        return redemptionService.redeem(phoneNumber, body.data())
                .map(ApiResponse::ok)
                .onErrorResume(ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }
}
