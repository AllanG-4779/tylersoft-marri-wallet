package net.tylersoft.events.service;

import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.events.common.EventAction;
import net.tylersoft.events.common.EventStatus;
import net.tylersoft.events.common.EventVisibility;
import net.tylersoft.events.common.EventTimeDisplay;
import net.tylersoft.events.dto.event.CreateEventRequest;
import net.tylersoft.events.dto.event.EventResponse;
import net.tylersoft.events.dto.event.EventStatusRequest;
import net.tylersoft.events.dto.event.UpdateEventRequest;
import net.tylersoft.events.model.Event;
import net.tylersoft.events.repository.EventCategoryRepository;
import net.tylersoft.events.repository.EventRepository;
import net.tylersoft.events.repository.TagRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final DatabaseClient databaseClient;

    public Mono<Page<EventResponse>> listAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created_at"));
        return Mono.zip(
                eventRepository.findAllByOrderByCreatedAtDesc(pageable).map(EventResponse::from).collectList(),
                eventRepository.count()
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    public Mono<Page<EventResponse>> listByOrganization(UUID organizationId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created_at"));
        return Mono.zip(
                eventRepository.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable).map(EventResponse::from).collectList(),
                eventRepository.countByOrganizationId(organizationId)
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    public Mono<Page<EventResponse>> listByStatus(EventStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "start_date"));
        return Mono.zip(
                eventRepository.findAllByStatusOrderByStartDateAsc(status, pageable).map(EventResponse::from).collectList(),
                eventRepository.countByStatus(status)
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    public Mono<EventResponse> getById(UUID id) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .map(EventResponse::from);
    }

    public Mono<EventResponse> create(CreateEventRequest req, String bannerUrl, String logoUrl, UUID createdBy) {
        String slug = req.slug() != null ? req.slug() : toSlug(req.title());

        Mono<Void> categoryCheck = req.categoryId() == null ? Mono.empty() :
                categoryRepository.existsById(req.categoryId())
                        .flatMap(exists -> exists ? Mono.empty() :
                                Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                                        "Category not found: " + req.categoryId())));

        return categoryCheck
                .then(eventRepository.existsByOrganizationIdAndSlug(req.organizationId(), slug))
                .flatMap(exists -> {
                    if (exists)
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "An event with this slug already exists for the organization"));
                    Event e = new Event();
                    e.setOrganizationId(req.organizationId());
                    e.setCreatedBy(createdBy);
                    e.setTitle(req.title());
                    e.setSlug(slug);
                    e.setCategoryId(req.categoryId());
                    e.setEventType(req.eventType());
                    e.setVisibility(req.visibility() != null ? req.visibility() : EventVisibility.PUBLIC);
                    e.setShortDescription(req.shortDescription());
                    e.setDescription(req.description());
                    e.setStatus(EventStatus.DRAFT);
                    e.setVenueName(req.venueName());
                    e.setVenueAddress(req.venueAddress());
                    e.setVenueCity(req.venueCity());
                    e.setVenueCountry(req.venueCountry());
                    e.setVenueLatitude(req.venueLatitude());
                    e.setVenueLongitude(req.venueLongitude());
                    e.setOnlineEventUrl(req.onlineEventUrl());
                    e.setStartDate(req.startDate());
                    e.setStartTime(req.startTime());
                    e.setEndDate(req.endDate());
                    e.setEndTime(req.endTime());
                    e.setTimezone(req.timezone() != null ? req.timezone() : "Africa/Nairobi");
                    e.setTimeDisplay(req.timeDisplay() != null ? req.timeDisplay() : EventTimeDisplay.START_AND_END);
                    e.setTotalCapacity(req.totalCapacity());
                    e.setBannerUrl(bannerUrl);
                    e.setLogoUrl(logoUrl);
                    e.setSalesStartAt(req.salesStartAt());
                    e.setSalesEndAt(req.salesEndAt());
                    e.setCloseSalesAtCapacity(req.closeSalesAtCapacity() != null ? req.closeSalesAtCapacity() : true);
                    e.setMinTicketsPerOrder(req.minTicketsPerOrder() != null ? req.minTicketsPerOrder() : 1);
                    e.setMaxTicketsPerOrder(req.maxTicketsPerOrder());
                    e.setAllowGroupPurchases(req.allowGroupPurchases() != null ? req.allowGroupPurchases() : true);
                    e.setShowRemainingTickets(req.showRemainingTickets() != null ? req.showRemainingTickets() : true);
                    e.setAllowMultipleEntries(req.allowMultipleEntries() != null ? req.allowMultipleEntries() : false);
                    e.setEnableCheckinsStaff(req.enableCheckinsStaff() != null ? req.enableCheckinsStaff() : true);
                    e.setMinAge(req.minAge());
                    e.setCreatedAt(OffsetDateTime.now());
                    e.setUpdatedAt(OffsetDateTime.now());
                    return eventRepository.save(e);
                })
                .map(EventResponse::from);
    }

    public Mono<EventResponse> update(UUID id, UpdateEventRequest req) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .flatMap(e -> {
                    if (e.getStatus() != EventStatus.DRAFT)
                        return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only DRAFT events can be edited"));
                    if (req.title() != null) e.setTitle(req.title());
                    if (req.slug() != null) e.setSlug(req.slug());
                    if (req.categoryId() != null) e.setCategoryId(req.categoryId());
                    if (req.eventType() != null) e.setEventType(req.eventType());
                    if (req.visibility() != null) e.setVisibility(req.visibility());
                    if (req.shortDescription() != null) e.setShortDescription(req.shortDescription());
                    if (req.description() != null) e.setDescription(req.description());
                    if (req.timezone() != null) e.setTimezone(req.timezone());
                    if (req.timeDisplay() != null) e.setTimeDisplay(req.timeDisplay());
                    if (req.totalCapacity() != null) e.setTotalCapacity(req.totalCapacity());
                    if (req.venueName() != null) e.setVenueName(req.venueName());
                    if (req.venueAddress() != null) e.setVenueAddress(req.venueAddress());
                    if (req.venueCity() != null) e.setVenueCity(req.venueCity());
                    if (req.venueCountry() != null) e.setVenueCountry(req.venueCountry());
                    if (req.venueLatitude() != null) e.setVenueLatitude(req.venueLatitude());
                    if (req.venueLongitude() != null) e.setVenueLongitude(req.venueLongitude());
                    if (req.onlineEventUrl() != null) e.setOnlineEventUrl(req.onlineEventUrl());
                    if (req.bannerUrl() != null) e.setBannerUrl(req.bannerUrl());
                    if (req.logoUrl() != null) e.setLogoUrl(req.logoUrl());
                    if (req.salesStartAt() != null) e.setSalesStartAt(req.salesStartAt());
                    if (req.salesEndAt() != null) e.setSalesEndAt(req.salesEndAt());
                    if (req.closeSalesAtCapacity() != null) e.setCloseSalesAtCapacity(req.closeSalesAtCapacity());
                    if (req.minTicketsPerOrder() != null) e.setMinTicketsPerOrder(req.minTicketsPerOrder());
                    if (req.maxTicketsPerOrder() != null) e.setMaxTicketsPerOrder(req.maxTicketsPerOrder());
                    if (req.allowGroupPurchases() != null) e.setAllowGroupPurchases(req.allowGroupPurchases());
                    if (req.showRemainingTickets() != null) e.setShowRemainingTickets(req.showRemainingTickets());
                    if (req.allowMultipleEntries() != null) e.setAllowMultipleEntries(req.allowMultipleEntries());
                    if (req.enableCheckinsStaff() != null) e.setEnableCheckinsStaff(req.enableCheckinsStaff());
                    if (req.minAge() != null) e.setMinAge(req.minAge());
                    if (req.startDate() != null) e.setStartDate(req.startDate());
                    if (req.startTime() != null) e.setStartTime(req.startTime());
                    if (req.endDate() != null) e.setEndDate(req.endDate());
                    if (req.endTime() != null) e.setEndTime(req.endTime());
                    e.setUpdatedAt(OffsetDateTime.now());
                    return eventRepository.save(e);
                })
                .map(EventResponse::from);
    }

    public Mono<EventResponse> changeStatus(UUID id, EventStatusRequest req, UUID userId) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .flatMap(e -> {
                    EventStatus next = resolveNextStatus(e.getStatus(), req.action());
                    e.setStatus(next);
                    e.setUpdatedAt(OffsetDateTime.now());
                    if (req.action() == EventAction.APPROVE) {
                        e.setApprovedBy(userId);
                        e.setApprovedAt(OffsetDateTime.now());
                        e.setApprovalNotes(req.notes());
                    }
                    if (req.action() == EventAction.REJECT) {
                        e.setRejectionReason(req.notes());
                    }
                    return eventRepository.save(e);
                })
                .map(EventResponse::from);
    }

    public Mono<Void> delete(UUID id) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .flatMap(e -> {
                    if (e.getStatus() != EventStatus.DRAFT)
                        return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only DRAFT events can be deleted"));
                    return eventRepository.delete(e);
                });
    }

    public Mono<Void> addTag(UUID eventId, UUID tagId) {
        return Mono.zip(
                        eventRepository.existsById(eventId),
                        tagRepository.existsById(tagId)
                )
                .flatMap(t -> {
                    if (!t.getT1())
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
                    if (!t.getT2())
                        return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Tag not found: " + tagId));
                    return databaseClient.sql("INSERT INTO event_tags (event_id, tag_id) VALUES (:eventId, :tagId) ON CONFLICT DO NOTHING")
                            .bind("eventId", eventId)
                            .bind("tagId", tagId)
                            .then();
                });
    }

    public Mono<Void> removeTag(UUID eventId, UUID tagId) {
        return databaseClient.sql("DELETE FROM event_tags WHERE event_id = :eventId AND tag_id = :tagId")
                .bind("eventId", eventId)
                .bind("tagId", tagId)
                .then();
    }

    private EventStatus resolveNextStatus(EventStatus current, EventAction action) {
        return switch (action) {
            case SUBMIT -> {
                if (current != EventStatus.DRAFT)
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only DRAFT events can be submitted");
                yield EventStatus.PENDING;
            }
            case APPROVE -> {
                if (current != EventStatus.PENDING)
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only PENDING events can be approved");
                yield EventStatus.APPROVED;
            }
            case REJECT -> {
                if (current != EventStatus.PENDING)
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only PENDING events can be rejected");
                yield EventStatus.DRAFT;
            }
            case START -> {
                if (current != EventStatus.APPROVED)
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only APPROVED events can be started");
                yield EventStatus.ONGOING;
            }
            case COMPLETE -> {
                if (current != EventStatus.ONGOING)
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only ONGOING events can be completed");
                yield EventStatus.COMPLETED;
            }
            case CANCEL -> {
                if (current == EventStatus.COMPLETED || current == EventStatus.CANCELLED)
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot cancel a " + current + " event");
                yield EventStatus.CANCELLED;
            }
        };
    }

    private static String toSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
