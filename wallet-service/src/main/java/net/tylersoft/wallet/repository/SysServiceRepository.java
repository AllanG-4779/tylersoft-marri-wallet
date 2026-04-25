package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.model.SysService;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface SysServiceRepository extends R2dbcRepository<SysService, Integer> {

    Mono<SysService> findByTransactionType(String transactionType);

    Mono<SysService> findByTransactionTypeAndIsEnquiryTrue(String transactionType);
}
