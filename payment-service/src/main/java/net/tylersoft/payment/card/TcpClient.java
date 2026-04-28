package net.tylersoft.payment.card;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.ReactiveHttpClient;
import net.tylersoft.payment.card.dto.TcpDeviceDataRequest;
import net.tylersoft.payment.card.dto.TcpDeviceDataResponse;
import net.tylersoft.payment.card.dto.TcpPaymentRequest;
import net.tylersoft.payment.card.dto.TcpPaymentResponse;
import net.tylersoft.payment.service.OutgoingRequestLogService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TcpClient {

    private final TcpProperties props;
    private final ReactiveHttpClient httpClient;
    private final OutgoingRequestLogService logService;

    public Mono<TcpDeviceDataResponse> deviceDataCollection(TcpDeviceDataRequest request) {
        String url = props.getBaseUrl() + "/tcp/api/deviceData-Collection";
        return logService.save(request.tranid(), "TCP_DEVICE_DATA", url, request)
                .flatMap(log -> httpClient.post(url, request, TcpDeviceDataResponse.class)
                        .flatMap(resp -> logService.updateSuccess(log.getId(), resp.statuscode(), resp).thenReturn(resp))
                        .onErrorResume(ex -> logService.updateFailure(log.getId(), ex.getMessage()).then(Mono.error(ex))));
    }

    public Mono<TcpPaymentResponse> payment(TcpPaymentRequest request) {
        String url = props.getBaseUrl() + "/tcp/api/request";
        return logService.save(request.tranid(), "TCP_PAYMENT", url, request)
                .flatMap(log -> httpClient.post(url, request, TcpPaymentResponse.class)
                        .flatMap(resp -> logService.updateSuccess(log.getId(), resp.statuscode(), resp).thenReturn(resp))
                        .onErrorResume(ex -> logService.updateFailure(log.getId(), ex.getMessage()).then(Mono.error(ex))));
    }
}
