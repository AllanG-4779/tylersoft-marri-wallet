package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.model.TransactionCharge;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface TransactionChargeRepository extends R2dbcRepository<TransactionCharge, Integer> {

    Flux<TransactionCharge> findByEsbRef(Long esbRef);

    Flux<TransactionCharge> findByChargeId(Integer chargeId);
}
