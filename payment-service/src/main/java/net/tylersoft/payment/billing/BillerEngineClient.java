package net.tylersoft.payment.billing;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.ReactiveHttpClient;
import net.tylersoft.payment.billing.dto.BillerTokenResponse;
import net.tylersoft.payment.billing.dto.BpcMeterRequest;
import net.tylersoft.payment.billing.dto.BpcMeterResponse;
import net.tylersoft.payment.billing.dto.VendRequest;
import net.tylersoft.payment.billing.dto.VendResponse;
import net.tylersoft.payment.config.BillingProperties;
import net.tylersoft.payment.service.OutgoingRequestLogService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class BillerEngineClient {

    private final BillingProperties props;
    private final ReactiveHttpClient httpClient;
    private final OutgoingRequestLogService logService;

    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private final AtomicLong tokenExpiryMs = new AtomicLong(0);

    public Mono<String> getAccessToken() {
        String token = cachedToken.get();
        if (token != null && System.currentTimeMillis() < tokenExpiryMs.get()) {
            return Mono.just(token);
        }
        return fetchToken();
    }

    private Mono<String> fetchToken() {
        String url = props.getBaseUrl() + props.resolveEndpoint(props.findService("ACCESS_TOKEN"));
        String encoded = Base64.getEncoder()
                .encodeToString((props.getUsername() + ":" + props.getPassword()).getBytes(StandardCharsets.UTF_8));

        return httpClient.post(url, Map.of("Authorization", "Basic " + encoded), null, BillerTokenResponse.class)
                .map(resp -> {
                    String token = resp.payload().accessToken();
                    cachedToken.set(token);
                    tokenExpiryMs.set(System.currentTimeMillis() + 6_900_000L); // 115 min
                    return token;
                });
    }

    public Mono<BpcMeterResponse> confirmMeter(String url, String meterNumber) {
        var request = new BpcMeterRequest(meterNumber, props.getClientId());

        return getAccessToken()
                .flatMap(token -> logService.save(meterNumber, "BPC_PRESENTMENT", url, request)
                        .flatMap(savedLog -> httpClient
                                .post(url, bearerHeaders(token), request, BpcMeterResponse.class)
                                .flatMap(resp -> logService
                                        .updateSuccess(savedLog.getId(), resp.status(), resp)
                                        .thenReturn(resp))
                                .onErrorResume(ex -> logService
                                        .updateFailure(savedLog.getId(), ex.getMessage())
                                        .then(Mono.error(ex)))));
    }

    public Mono<VendResponse> vend(String url, String serviceCode, VendRequest request) {
        return getAccessToken()
                .flatMap(token -> logService.save(request.transactionId(), serviceCode, url, request)
                        .flatMap(savedLog -> httpClient
                                .post(url, bearerHeaders(token), request, VendResponse.class)
                                .flatMap(resp -> logService
                                        .updateSuccess(savedLog.getId(), resp.responseCode(), resp)
                                        .thenReturn(resp))
                                .onErrorResume(ex -> logService
                                        .updateFailure(savedLog.getId(), ex.getMessage())
                                        .then(Mono.error(ex)))));
    }

    private Map<String, String> bearerHeaders(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }
}
