package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.model.TrxMessage;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TrxMessageRepository extends R2dbcRepository<TrxMessage, Long> {

    Mono<TrxMessage> findByTransactionRef(String transactionRef);

    Flux<TrxMessage> findByPhoneNumber(String phoneNumber);

    Flux<TrxMessage> findByDebitAccount(String debitAccount);

    Flux<TrxMessage> findByCreditAccount(String creditAccount);

    @Query("SELECT * FROM trx_messages WHERE (debit_account = :account OR credit_account = :account) ORDER BY created_on DESC LIMIT :limit")
    Flux<TrxMessage> findMiniStatement(String account, int limit);

    @Query("SELECT * FROM trx_messages WHERE (debit_account = :account OR credit_account = :account) AND created_on BETWEEN :from AND :to ORDER BY created_on DESC")
    Flux<TrxMessage> findStatement(String account, java.time.OffsetDateTime from, java.time.OffsetDateTime to);

    @Modifying
    @Query("UPDATE trx_messages SET status = :status, response_code = :responseCode, response_message = :responseMessage, updated_on = NOW() WHERE id = :id")
    Mono<Integer> updateStatus(Long id, Short status, String responseCode, String responseMessage);
}
