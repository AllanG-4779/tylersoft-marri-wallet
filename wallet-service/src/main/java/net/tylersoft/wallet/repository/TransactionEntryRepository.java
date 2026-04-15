package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.model.TransactionEntry;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface TransactionEntryRepository extends R2dbcRepository<TransactionEntry, Integer> {

    Flux<TransactionEntry> findByEsbRef(Long esbRef);

    Flux<TransactionEntry> findByAccountNumber(String accountNumber);

    Flux<TransactionEntry> findByAccountNumberOrderByCreatedOnDesc(String accountNumber);
}
