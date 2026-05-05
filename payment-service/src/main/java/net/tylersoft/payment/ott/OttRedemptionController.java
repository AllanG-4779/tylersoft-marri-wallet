package net.tylersoft.payment.ott;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.payment.ott.dto.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ott/redemption")
@RequiredArgsConstructor
public class OttRedemptionController {

    private final OttRedemptionService ottRedemptionService;

    @PostMapping("/check-voucher")
    public Mono<ApiResponse<OttCheckVoucherResponse>> checkVoucher(
            @Valid @RequestBody OttCheckVoucherRequest request) {
        return ottRedemptionService.checkVoucher(request)
                .map(ApiResponse::ok)
                .onErrorResume(ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }

    @PostMapping("/remit-voucher")
    public Mono<ApiResponse<OttRemitVoucherResponse>> remitVoucher(
            @Valid @RequestBody OttRemitVoucherRequest request) {
        return ottRedemptionService.remitVoucher(request)
                .map(ApiResponse::ok)
                .onErrorResume(ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }

    @PostMapping("/check-remit")
    public Mono<ApiResponse<OttCheckRemitVoucherResponse>> checkRemitVoucher(
            @Valid @RequestBody OttCheckRemitVoucherRequest request) {
        return ottRedemptionService.checkRemitVoucher(request)
                .map(ApiResponse::ok)
                .onErrorResume(ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }
}
