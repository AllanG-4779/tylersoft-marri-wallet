package net.tylersoft.payment.controller;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.payment.billing.BillerEngineService;
import net.tylersoft.payment.billing.api.BpcConfirmRequest;
import net.tylersoft.payment.billing.api.VendApiRequest;
import net.tylersoft.payment.billing.dto.BpcMeterResponse;
import net.tylersoft.payment.billing.dto.VendResponse;
import net.tylersoft.payment.model.OutgoingRequestLog;
import net.tylersoft.payment.service.OutgoingRequestLogService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillerController {

    private final BillerEngineService billerService;
    private final OutgoingRequestLogService logService;

    @PostMapping("/bpc/confirm-meter")
    public Mono<BpcMeterResponse> confirmMeter(@RequestBody BpcConfirmRequest request) {
        return billerService.confirmBpcMeter(request.meterNumber());
    }

    @PostMapping("/vend")
    public Mono<VendResponse> vend(@RequestBody VendApiRequest request) {
        return billerService.processVend(
                request.serviceCode(),
                request.accountNo(),
                request.amount(),
                request.currency(),
                request.phoneNumber(),
                request.transactionId(),
                request.email());
    }

    @PostMapping("/callback/{referenceId}")
    public Mono<Void> handleCallback(@PathVariable String referenceId, @RequestBody String payload) {
        return logService.updateCallback(referenceId, payload);
    }

    @GetMapping("/transaction/{referenceId}/status")
    public Mono<ApiResponse<String>> getTransactionStatus(@PathVariable String referenceId) {
        return logService.getCallbackStatus(referenceId)
                .map(log -> ApiResponse.ok(log.getStatus()))
                .defaultIfEmpty(ApiResponse.error("No transaction found for reference: " + referenceId));
    }
}
