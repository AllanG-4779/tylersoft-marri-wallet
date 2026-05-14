package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.events.common.EventStatus;
import net.tylersoft.events.dto.event.CreateEventRequest;
import net.tylersoft.events.dto.event.EventResponse;
import net.tylersoft.events.dto.event.UpdateEventRequest;
import net.tylersoft.events.model.Event;
import net.tylersoft.events.repository.EventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public Mono<EventResponse> create(CreateEventRequest req, String createdBy) {
        Event event = new Event();
        event.setMerchantCode(req.merchantCode());
        event.setTitle(req.title());
        event.setDescription(req.description());
        event.setVenueName(req.venueName());
        event.setVenueAddress(req.venueAddress());
        event.setStartAt(req.startAt());
        event.setEndAt(req.endAt());
        event.setCoverImageUrl(req.coverImageUrl());
        event.setStatus(EventStatus.DRAFT.name());
        event.setStatusChangedAt(OffsetDateTime.now());
        event.setCreatedBy(createdBy);
        event.setCreatedAt(OffsetDateTime.now());
        event.setUpdatedAt(OffsetDateTime.now());
        return eventRepository.save(event).map(EventResponse::from);
    }

    public Mono<EventResponse> getById(UUID id) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .map(EventResponse::from);
    }

    public Mono<Page<EventResponse>> listAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Mono.zip(
                eventRepository.findAllByOrderByCreatedAtDesc(pageable).map(EventResponse::from).collectList(),
                eventRepository.count()
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    public Mono<Page<EventResponse>> listByStatus(String status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Mono.zip(
                eventRepository.findAllByStatusOrderByStartAtAsc(status, pageable).map(EventResponse::from).collectList(),
                eventRepository.countByStatus(status)
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    public Mono<Page<EventResponse>> listByMerchant(String merchantCode, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Mono.zip(
                eventRepository.findAllByMerchantCodeOrderByCreatedAtDesc(merchantCode, pageable).map(EventResponse::from).collectList(),
                eventRepository.countByMerchantCode(merchantCode)
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    public Mono<EventResponse> update(UUID id, UpdateEventRequest req) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .flatMap(event -> {
                    if (!EventStatus.DRAFT.name().equals(event.getStatus()))
                        return Mono.error(new IllegalArgumentException("Only DRAFT events can be updated"));
                    if (req.title() != null) event.setTitle(req.title());
                    if (req.description() != null) event.setDescription(req.description());
                    if (req.venueName() != null) event.setVenueName(req.venueName());
                    if (req.venueAddress() != null) event.setVenueAddress(req.venueAddress());
                    if (req.startAt() != null) event.setStartAt(req.startAt());
                    if (req.endAt() != null) event.setEndAt(req.endAt());
                    if (req.coverImageUrl() != null) event.setCoverImageUrl(req.coverImageUrl());
                    event.setUpdatedAt(OffsetDateTime.now());
                    return eventRepository.save(event);
                })
                .map(EventResponse::from);
    }

    public Mono<EventResponse> updateStatus(UUID id, String action, String reason, String updatedBy) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .flatMap(event -> resolveNextStatus(event.getStatus(), action)
                        .flatMap(next -> {
                            event.setStatus(next);
                            event.setStatusReason(reason);
                            event.setStatusChangedAt(OffsetDateTime.now());
                            event.setUpdatedAt(OffsetDateTime.now());
                            return eventRepository.save(event);
                        })
                )
                .map(EventResponse::from);
    }

    private Mono<String> resolveNextStatus(String current, String action) {
        return switch (action.toUpperCase()) {
            case "PUBLISH" -> EventStatus.DRAFT.name().equals(current)
                    ? Mono.just(EventStatus.PUBLISHED.name())
                    : Mono.error(new IllegalArgumentException("Only DRAFT events can be published"));

            case "START" -> EventStatus.PUBLISHED.name().equals(current)
                    ? Mono.just(EventStatus.ONGOING.name())
                    : Mono.error(new IllegalArgumentException("Only PUBLISHED events can be started"));

            case "COMPLETE" -> EventStatus.ONGOING.name().equals(current)
                    ? Mono.just(EventStatus.COMPLETED.name())
                    : Mono.error(new IllegalArgumentException("Only ONGOING events can be completed"));

            case "CANCEL" -> (EventStatus.COMPLETED.name().equals(current) || EventStatus.CANCELLED.name().equals(current))
                    ? Mono.error(new IllegalArgumentException("Cannot cancel a " + current + " event"))
                    : Mono.just(EventStatus.CANCELLED.name());

            default -> Mono.error(new IllegalArgumentException("action must be PUBLISH, START, COMPLETE, or CANCEL"));
        };
    }
}
