package net.tylersoft.payment.billing;

import lombok.RequiredArgsConstructor;
import net.tylersoft.payment.billing.dto.BpcMeterResponse;
import net.tylersoft.payment.billing.dto.VendRequest;
import net.tylersoft.payment.billing.dto.VendResponse;
import net.tylersoft.payment.config.BillingProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BillerEngineService {

    private final BillerEngineClient client;
    private final BillingProperties props;

    public Mono<BpcMeterResponse> confirmBpcMeter(String meterNumber) {
        String url = props.getBaseUrl() + props.resolveEndpoint(props.findService("BPC_PRESENTMENT"));
        return client.confirmMeter(url, meterNumber);
    }

    public Mono<VendResponse> processVend(String serviceCode, String accountNo, String amount,
                                          String currency, String phoneNumber,
                                          String transactionId, String email) {
        var service = props.findService(serviceCode);
        String url = props.getBaseUrl() + props.resolveEndpoint(service);
        var request = new VendRequest(
                amount,
                currency != null ? currency : "BWP",
                props.getClientId(),
                service.serviceId(),
                phoneNumber,
                accountNo,
                transactionId,
                email
        );
        return client.vend(url, serviceCode, request);
    }
}
