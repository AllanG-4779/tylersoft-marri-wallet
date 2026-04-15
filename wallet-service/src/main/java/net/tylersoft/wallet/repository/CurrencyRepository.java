package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.model.Currency;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface CurrencyRepository extends R2dbcRepository<Currency, Integer> {

    Mono<Currency> findByCurrencyCode(String currencyCode);

    Mono<Currency> findByIsoCode(String isoCode);
}
