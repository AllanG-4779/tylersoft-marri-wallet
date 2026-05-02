package net.tylersoft.payment.ott;

import lombok.RequiredArgsConstructor;
import net.tylersoft.payment.ott.dto.OttVoucherRequest;
import net.tylersoft.payment.ott.dto.OttVoucherResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OttService {

    private final OttClient client;

    public Mono<OttVoucherResponse> getVoucher(OttVoucherRequest request) {
        return client.getVoucher(request);
    }
}
