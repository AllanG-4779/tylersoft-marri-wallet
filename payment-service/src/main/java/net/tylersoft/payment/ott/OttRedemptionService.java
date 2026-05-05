package net.tylersoft.payment.ott;

import lombok.RequiredArgsConstructor;
import net.tylersoft.payment.ott.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OttRedemptionService {

    private final OttRedemptionClient client;

    public Mono<OttCheckVoucherResponse> checkVoucher(OttCheckVoucherRequest request) {
        return client.checkVoucher(request);
    }

    public Mono<OttRemitVoucherResponse> remitVoucher(OttRemitVoucherRequest request) {
        return client.remitVoucher(request);
    }

    public Mono<OttCheckRemitVoucherResponse> checkRemitVoucher(OttCheckRemitVoucherRequest request) {
        return client.checkRemitVoucher(request);
    }
}
