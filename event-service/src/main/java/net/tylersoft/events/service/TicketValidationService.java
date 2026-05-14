package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.events.common.TicketStatus;
import net.tylersoft.events.common.ValidationResult;
import net.tylersoft.events.dto.validation.ValidationResponse;
import net.tylersoft.events.model.TicketValidation;
import net.tylersoft.events.repository.EventRepository;
import net.tylersoft.events.repository.EventTicketRepository;
import net.tylersoft.events.repository.TicketValidationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketValidationService {

    private final EventRepository eventRepository;
    private final EventTicketRepository ticketRepository;
    private final TicketValidationRepository validationRepository;
    private final TransactionalOperator transactionalOperator;

    public Mono<ValidationResponse> validate(String ticketCode, UUID eventId, String validatedBy) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .flatMap(event -> {
                    OffsetDateTime now = OffsetDateTime.now();

                    if (now.isBefore(event.getStartAt()))
                        return logAndReturn(ticketCode, eventId, null, validatedBy,
                                ValidationResult.EVENT_NOT_STARTED, "Event has not started yet");

                    if (now.isAfter(event.getEndAt()))
                        return logAndReturn(ticketCode, eventId, null, validatedBy,
                                ValidationResult.EVENT_ENDED, "Event has ended");

                    return ticketRepository.findByTicketCode(ticketCode)
                            .flatMap(ticket -> {
                                if (!ticket.getEventId().equals(eventId))
                                    return logAndReturn(ticketCode, eventId, null, validatedBy,
                                            ValidationResult.NOT_FOUND, "Ticket does not belong to this event");

                                String status = ticket.getStatus();
                                if (TicketStatus.USED.name().equals(status))
                                    return logAndReturn(ticketCode, eventId, ticket.getId(), validatedBy,
                                            ValidationResult.ALREADY_USED, "Ticket has already been used");
                                if (TicketStatus.CANCELLED.name().equals(status))
                                    return logAndReturn(ticketCode, eventId, ticket.getId(), validatedBy,
                                            ValidationResult.CANCELLED, "Ticket has been cancelled");
                                if (TicketStatus.EXPIRED.name().equals(status))
                                    return logAndReturn(ticketCode, eventId, ticket.getId(), validatedBy,
                                            ValidationResult.EXPIRED, "Ticket has expired");

                                OffsetDateTime stamp = OffsetDateTime.now();
                                ticket.setStatus(TicketStatus.USED.name());
                                ticket.setUsedAt(stamp);
                                ticket.setUpdatedAt(stamp);
                                return ticketRepository.save(ticket)
                                        .flatMap(t -> logAndReturn(ticketCode, eventId, t.getId(), validatedBy,
                                                ValidationResult.VALID, "Ticket validated successfully"));
                            })
                            .switchIfEmpty(logAndReturn(ticketCode, eventId, null, validatedBy,
                                    ValidationResult.NOT_FOUND, "Ticket code not found"));
                })
                .as(transactionalOperator::transactional);
    }

    public Mono<Page<ValidationResponse>> getValidations(UUID eventId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "validatedAt"));
        return Mono.zip(
                validationRepository.findAllByEventIdOrderByValidatedAtDesc(eventId, pageable)
                        .map(this::toResponse).collectList(),
                validationRepository.countByEventId(eventId)
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private Mono<ValidationResponse> logAndReturn(String ticketCode, UUID eventId, UUID ticketId,
                                                   String validatedBy, ValidationResult result, String message) {
        TicketValidation log = new TicketValidation();
        log.setTicketCode(ticketCode);
        log.setEventId(eventId);
        log.setTicketId(ticketId);
        log.setValidatedBy(validatedBy);
        log.setResult(result.name());
        log.setNotes(message);
        log.setValidatedAt(OffsetDateTime.now());
        return validationRepository.save(log)
                .map(saved -> new ValidationResponse(ticketCode, result.name(), message,
                        ticketId, eventId, saved.getValidatedAt()));
    }

    private ValidationResponse toResponse(TicketValidation v) {
        return new ValidationResponse(v.getTicketCode(), v.getResult(), v.getNotes(),
                v.getTicketId(), v.getEventId(), v.getValidatedAt());
    }
}
