package net.tylersoft.wallet.quote;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

interface TransactionQuoteRepository extends R2dbcRepository<TransactionQuote, Long> {
    Mono<TransactionQuote> findByToken(String token);
}
