package net.tylersoft.payment.ott;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.payment.ott.dto.OttVoucherRequest;
import net.tylersoft.payment.ott.dto.OttVoucherResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ott")
@RequiredArgsConstructor
public class OttController {

    private final OttService ottService;

    @PostMapping("/voucher")
    public Mono<ApiResponse<OttVoucherResponse>> getVoucher(
            @Valid @RequestBody OttVoucherRequest request) {
        return ottService.getVoucher(request)
                .map(ApiResponse::ok)
                .onErrorResume(ex -> Mono.just(ApiResponse.error(ex.getMessage())));
    }
}
