package net.tylersoft.payment.card;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.http.dto.ApiResponse;
import net.tylersoft.payment.card.api.CardDeviceDataRequest;
import net.tylersoft.payment.card.api.CardPaymentRequest;
import net.tylersoft.payment.card.dto.TcpCallbackPayload;
import net.tylersoft.payment.card.dto.TcpDeviceDataResponse;
import net.tylersoft.payment.card.dto.TcpPaymentResponse;
import net.tylersoft.payment.service.OutgoingRequestLogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/card")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final OutgoingRequestLogService logService;

    @PostMapping("/device-fingerprint")
    public Mono<ApiResponse<TcpDeviceDataResponse>> deviceFingerprint(
            @Validated @RequestBody CardDeviceDataRequest request) {
        return cardService.deviceDataCollection(request)
                .map(ApiResponse::ok);
    }

    @PostMapping("/payment")
    public Mono<ApiResponse<TcpPaymentResponse>> payment(
            @Validated @RequestBody CardPaymentRequest request) {
        return cardService.payment(request)
                .map(ApiResponse::ok);
    }

    @PostMapping("/callback")
    public Mono<Map<String, String>> callback(@RequestBody TcpCallbackPayload payload) {
        log.info("CARD PAYMENT CALLBACK {} {} {}", payload.tranid(), payload.status(), payload.statuscode());
        return logService.updateCallback(payload.tranid(), payload.toString())
                .thenReturn(Map.of("statuscode", "00", "tranid", payload.tranid()));
    }
}
