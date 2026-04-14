package net.tylersoft.wallet.service;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface SystemServiceRepository extends R2dbcRepository<SystemService, Integer> {

    Mono<SystemService> findByTransactionType(String transactionType);
}
