package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.events.common.PlannerStatus;
import net.tylersoft.events.dto.planner.AddPlannerRequest;
import net.tylersoft.events.dto.planner.PlannerResponse;
import net.tylersoft.events.model.EventPlanner;
import net.tylersoft.events.repository.EventPlannerRepository;
import net.tylersoft.events.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventPlannerService {

    private final EventPlannerRepository plannerRepository;
    private final EventRepository eventRepository;

    public Mono<PlannerResponse> add(UUID eventId, AddPlannerRequest req) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .flatMap(event -> plannerRepository.existsByEventIdAndCustomerId(eventId, req.customerId()))
                .flatMap(exists -> {
                    if (exists) return Mono.error(new IllegalArgumentException("Customer is already a planner for this event"));
                    EventPlanner planner = new EventPlanner();
                    planner.setEventId(eventId);
                    planner.setCustomerId(req.customerId());
                    planner.setName(req.name());
                    planner.setEmail(req.email());
                    planner.setPhone(req.phone());
                    planner.setRole(req.role());
                    planner.setStatus(PlannerStatus.ACTIVE.name());
                    planner.setCreatedAt(OffsetDateTime.now());
                    planner.setUpdatedAt(OffsetDateTime.now());
                    return plannerRepository.save(planner);
                })
                .map(PlannerResponse::from);
    }

    public Flux<PlannerResponse> listByEvent(UUID eventId) {
        return plannerRepository.findAllByEventId(eventId).map(PlannerResponse::from);
    }

    public Mono<EventPlanner> remove(UUID eventId, UUID customerId) {
        return plannerRepository.findByEventIdAndCustomerId(eventId, customerId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Planner not found for this event")))
                .flatMap(planner -> {
                    planner.setStatus(PlannerStatus.REMOVED.name());
                    planner.setUpdatedAt(OffsetDateTime.now());
                    return plannerRepository.save(planner);
                });
    }

    public Mono<Boolean> isActivePlanner(UUID eventId, UUID customerId) {
        return plannerRepository.findByEventIdAndCustomerId(eventId, customerId)
                .map(p -> PlannerStatus.ACTIVE.name().equals(p.getStatus()))
                .defaultIfEmpty(false);
    }
}
