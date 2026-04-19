package net.tylersoft.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.logging.LogSanitizer;
import net.tylersoft.payment.model.OutgoingRequestLog;
import net.tylersoft.payment.repository.OutgoingRequestLogRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutgoingRequestLogService {

    private final OutgoingRequestLogRepository repository;
    private final LogSanitizer logSanitizer;

    public Mono<OutgoingRequestLog> save(String referenceId, String serviceCode, String endpoint, Object requestPayload) {
        var entry = new OutgoingRequestLog();
        entry.setReferenceId(referenceId);
        entry.setServiceCode(serviceCode);
        entry.setEndpoint(endpoint);
        entry.setRequestPayload(toJson(requestPayload));
        entry.setStatus("PENDING");
        entry.setCreatedOn(OffsetDateTime.now());
        return repository.save(entry)
                .onErrorResume(ex -> {
                    log.error("Failed to persist outgoing request log [service={}]: {}", serviceCode, ex.getMessage());
                    return Mono.just(entry); // proceed without a saved id
                });
    }

    public Mono<Void> updateSuccess(Long id, String responseCode, Object responsePayload) {
        if (id == null) return Mono.empty();
        return repository.findById(id)
                .flatMap(entry -> {
                    entry.setStatus("SUCCESS");
                    entry.setResponseCode(responseCode);
                    entry.setResponsePayload(toJson(responsePayload));
                    entry.setUpdatedOn(OffsetDateTime.now());
                    return repository.save(entry);
                })
                .onErrorResume(ex -> {
                    log.error("Failed to update success log [id={}]: {}", id, ex.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    public Mono<Void> updateFailure(Long id, String errorMessage) {
        if (id == null) return Mono.empty();
        return repository.findById(id)
                .flatMap(entry -> {
                    entry.setStatus("FAILED");
                    entry.setErrorMessage(errorMessage);
                    entry.setUpdatedOn(OffsetDateTime.now());
                    return repository.save(entry);
                })
                .onErrorResume(ex -> {
                    log.error("Failed to update failure log [id={}]: {}", id, ex.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    public Mono<Void> updateCallback(String referenceId, String callbackPayload) {
        return repository.findTopByReferenceIdOrderByCreatedOnDesc(referenceId)
                .flatMap(entry -> {
                    entry.setStatus("CALLBACK_RECEIVED");
                    entry.setCallbackPayload(callbackPayload);
                    entry.setCallbackReceivedOn(OffsetDateTime.now());
                    entry.setUpdatedOn(OffsetDateTime.now());
                    return repository.save(entry);
                })
                .onErrorResume(ex -> {
                    log.error("Failed to update callback log [referenceId={}]: {}", referenceId, ex.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    public Mono<OutgoingRequestLog> getCallbackStatus(String referenceId) {
        return repository.findTopByReferenceIdAndStatusOrderByCreatedOnDesc(referenceId, "CALLBACK_RECEIVED")
                .switchIfEmpty(repository.findTopByReferenceIdOrderByCreatedOnDesc(referenceId));
    }

    private String toJson(Object obj) {
        return logSanitizer.toJson(obj);
    }
}
