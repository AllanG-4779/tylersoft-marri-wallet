package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.model.AccountType;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface AccountTypeRepository extends R2dbcRepository<AccountType, Integer> {

    Mono<AccountType> findByTypeName(String typeName);

    Mono<AccountType> findByAccountPrefix(String accountPrefix);
}
