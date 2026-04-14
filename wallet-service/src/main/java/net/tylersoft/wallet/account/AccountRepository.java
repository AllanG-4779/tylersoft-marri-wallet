package net.tylersoft.wallet.account;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountRepository extends R2dbcRepository<Account, Long> {

    Mono<Account> findByAccountNumber(String accountNumber);

    Flux<Account> findByPhoneNumber(String phoneNumber);

    Mono<Boolean> existsByAccountNumber(String accountNumber);

    @Query("SELECT * FROM acc_accounts WHERE account_number = :accountNumber FOR UPDATE")
    Mono<Account> findByAccountNumberForUpdate(String accountNumber);
}
