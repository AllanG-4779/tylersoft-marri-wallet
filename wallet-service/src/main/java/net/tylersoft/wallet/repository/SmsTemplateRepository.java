package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.model.SmsTemplate;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface SmsTemplateRepository extends R2dbcRepository<SmsTemplate, Integer> {

    Mono<SmsTemplate> findByTransactionTypeAndTransactionCodeAndDirectionAndStatus(
            String transactionType, String transactionCode, String direction, String status);
}
