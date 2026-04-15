package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.model.SystemService;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface SystemServiceRepository extends R2dbcRepository<SystemService, Integer> {

    Mono<SystemService> findByTransactionType(String transactionType);
}
