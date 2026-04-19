package net.tylersoft.payment.repository;

import net.tylersoft.payment.model.OutgoingRequestLog;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface OutgoingRequestLogRepository extends R2dbcRepository<OutgoingRequestLog, Long> {

    Mono<OutgoingRequestLog> findTopByReferenceIdOrderByCreatedOnDesc(String referenceId);

    Mono<OutgoingRequestLog> findTopByReferenceIdAndStatusOrderByCreatedOnDesc(String referenceId, String status);
}
