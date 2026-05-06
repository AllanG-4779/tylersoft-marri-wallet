package net.tylersoft.wallet.ott;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class PaymentServiceOttRedemptionGateway implements OttRedemptionGatewayPort {

    private final WebClient webClient;

    public PaymentServiceOttRedemptionGateway(@Qualifier("paymentServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<OttCheckVoucherGatewayResult> checkVoucher(String voucherPin) {
        log.info("OTT check voucher → payment-service pin={}****", voucherPin.substring(0, Math.min(4, voucherPin.length())));

        return webClient.post()
                .uri("/api/v1/ott/redemption/check-voucher")
                .bodyValue(Map.of("voucherPin", voucherPin))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WsCheckVoucherApiResponse>() {})

                .map(resp -> {
                    WsCheckVoucherData d = resp.data();
                    boolean ok = "00".equals(resp.status()) && d != null && d.success();
                    log.info("OTT check voucher response status={} success={}", resp.status(), ok);
                    return new OttCheckVoucherGatewayResult(
                            ok,
                            d != null ? d.serial()    : null,
                            d != null ? d.voucherId()  : null,
                            d != null ? d.value()      : null,
                            d != null ? d.message()    : (resp.error() != null ? resp.error() : resp.message()),
                            d != null ? d.errorCode()  : null);
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty response from OTT gateway")))
                .onErrorResume(ex -> {
                    log.error("OTT check voucher gateway error", ex);
                    return Mono.just(new OttCheckVoucherGatewayResult(
                            false, null, null, null, "Gateway error: " + ex.getMessage(), null));
                });
    }

    @Override
    public Mono<OttRemitGatewayResult> remitVoucher(OttRemitGatewayRequest request) {
        log.info("OTT remit voucher → payment-service ref={} mobile={} amount={}",
                request.uniqueReference(), request.mobile(), request.amount());

        return webClient.post()
                .uri("/api/v1/ott/redemption/remit-voucher")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WsRemitApiResponse>() {})
                .map(resp -> mapRemit(resp.status(), resp.message(), resp.data(), resp.error()))
                .onErrorResume(ex -> {
                    log.error("OTT remit voucher gateway error ref={}", request.uniqueReference(), ex);
                    return Mono.just(new OttRemitGatewayResult(
                            false, null, null, null, null, "OTR99", "Gateway error: " + ex.getMessage()));
                });
    }

    @Override
    public Mono<OttRemitGatewayResult> checkRemitVoucher(String uniqueReference) {
        log.info("OTT check remit voucher → payment-service ref={}", uniqueReference);

        return webClient.post()
                .uri("/api/v1/ott/redemption/check-remit")
                .bodyValue(Map.of("uniqueReference", uniqueReference))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WsCheckRemitApiResponse>() {})
                .map(resp -> {
                    WsCheckRemitData d = resp.data();
                    boolean ok = "00".equals(resp.status()) && d != null && d.success();
                    return new OttRemitGatewayResult(
                            ok,
                            d != null ? d.voucherId()     : null,
                            d != null ? d.voucherAmount()  : null,
                            d != null ? d.voucherBalance() : null,
                            d != null ? d.serialNumber()   : null,
                            d != null ? d.errorCode()      : null,
                            d != null ? d.message()        : (resp.error() != null ? resp.error() : resp.message()));
                })
                .onErrorResume(ex -> {
                    log.error("OTT check remit gateway error ref={}", uniqueReference, ex);
                    return Mono.just(new OttRemitGatewayResult(
                            false, null, null, null, null, "OTR99", "Gateway error: " + ex.getMessage()));
                });
    }

    private OttRemitGatewayResult mapRemit(String status, String message, WsRemitData d, String error) {
        boolean ok = "00".equals(status) && d != null && d.success();
        log.info("OTT remit voucher response status={} success={}", status, ok);
        return new OttRemitGatewayResult(
                ok,
                d != null ? d.voucherId()     : null,
                d != null ? d.voucherAmount()  : null,
                d != null ? d.voucherBalance() : null,
                null,
                d != null ? d.errorCode()      : null,
                d != null ? d.message()        : (error != null ? error : message));
    }
}
