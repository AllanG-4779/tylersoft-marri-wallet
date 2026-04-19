package net.tylersoft.wallet.transfer;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.wallet.common.FTRequest;
import net.tylersoft.common.http.dto.UniversalRequestWrapper;
import net.tylersoft.wallet.service.FundTransferService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final FundTransferService fundTransferService;

    @PostMapping("/ft")
    public Mono<ApiResponse<FTResponse>> fundTransfer(
            @RequestBody UniversalRequestWrapper<FTRequest> request) {
        return processTransaction(request);
    }

    private Mono<ApiResponse<FTResponse>> processTransaction(@RequestBody UniversalRequestWrapper<FTRequest> request) {
        return fundTransferService.execute(request.data())
                .map(ctx -> {
                    var msg = ctx.getStagedMessage();
                    if (ctx.isSuccessful()) {
                        return ApiResponse.ok(new FTResponse(
                                msg.getTransactionRef(),
                                msg.getResponseCode(),
                                msg.getResponseMessage()));
                    }
                    return ApiResponse.<FTResponse>error(
                            ctx.getFailureCode() + " - " + ctx.getFailureMessage());
                });
    }

    @PostMapping("/card-topup")
    public Mono<ApiResponse<FTResponse>> cardTopUp(
            @RequestBody UniversalRequestWrapper<FTRequest> request) {
        return processTransaction(request);
    }
}
