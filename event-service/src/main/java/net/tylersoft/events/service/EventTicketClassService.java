package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.events.common.TicketClassStatus;
import net.tylersoft.events.dto.ticketclass.CreateTicketClassRequest;
import net.tylersoft.events.dto.ticketclass.TicketClassResponse;
import net.tylersoft.events.dto.ticketclass.UpdateTicketClassRequest;
import net.tylersoft.events.model.EventTicketClass;
import net.tylersoft.events.repository.EventRepository;
import net.tylersoft.events.repository.EventTicketClassRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventTicketClassService {

    private final EventTicketClassRepository ticketClassRepository;
    private final EventRepository eventRepository;

    public Mono<TicketClassResponse> add(UUID eventId, CreateTicketClassRequest req) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .flatMap(event -> {
                    EventTicketClass tc = new EventTicketClass();
                    tc.setEventId(eventId);
                    tc.setName(req.name());
                    tc.setDescription(req.description());
                    tc.setPrice(req.price());
                    tc.setCurrency(req.currency() != null ? req.currency() : "ZMW");
                    tc.setCapacity(req.capacity());
                    tc.setSoldCount(0);
                    tc.setStatus(TicketClassStatus.ACTIVE.name());
                    tc.setCreatedAt(OffsetDateTime.now());
                    tc.setUpdatedAt(OffsetDateTime.now());
                    return ticketClassRepository.save(tc);
                })
                .map(TicketClassResponse::from);
    }

    public Mono<TicketClassResponse> update(UUID classId, UpdateTicketClassRequest req) {
        return ticketClassRepository.findById(classId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket class not found")))
                .flatMap(tc -> {
                    if (req.name() != null) tc.setName(req.name());
                    if (req.description() != null) tc.setDescription(req.description());
                    if (req.price() != null) tc.setPrice(req.price());
                    if (req.capacity() != null) {
                        if (req.capacity() < tc.getSoldCount())
                            return Mono.error(new IllegalArgumentException(
                                    "Capacity cannot be less than tickets already sold (" + tc.getSoldCount() + ")"));
                        tc.setCapacity(req.capacity());
                    }
                    tc.setUpdatedAt(OffsetDateTime.now());
                    return ticketClassRepository.save(tc);
                })
                .map(TicketClassResponse::from);
    }

    public Flux<TicketClassResponse> listByEvent(UUID eventId) {
        return ticketClassRepository.findAllByEventId(eventId).map(TicketClassResponse::from);
    }

    public Mono<TicketClassResponse> deactivate(UUID classId) {
        return ticketClassRepository.findById(classId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket class not found")))
                .flatMap(tc -> {
                    tc.setStatus(TicketClassStatus.INACTIVE.name());
                    tc.setUpdatedAt(OffsetDateTime.now());
                    return ticketClassRepository.save(tc);
                })
                .map(TicketClassResponse::from);
    }
}
