package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.events.dto.tickettype.CreateTicketTypeRequest;
import net.tylersoft.events.dto.tickettype.TicketTypeResponse;
import net.tylersoft.events.dto.tickettype.UpdateTicketTypeRequest;
import net.tylersoft.events.model.TicketType;
import net.tylersoft.events.repository.EventRepository;
import net.tylersoft.events.repository.TicketTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;

    public Flux<TicketTypeResponse> listByEvent(UUID eventId) {
        return ticketTypeRepository.findAllByEventIdOrderBySortOrderAsc(eventId).map(TicketTypeResponse::from);
    }

    public Mono<TicketTypeResponse> getById(UUID id) {
        return ticketTypeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket type not found")))
                .map(TicketTypeResponse::from);
    }

    public Mono<TicketTypeResponse> create(UUID eventId, CreateTicketTypeRequest req) {
        return eventRepository.existsById(eventId)
                .flatMap(exists -> {
                    if (!exists) return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
                    return ticketTypeRepository.existsByEventIdAndNameIgnoreCase(eventId, req.name());
                })
                .flatMap(nameTaken -> {
                    if (nameTaken)
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
                                "A ticket type named '" + req.name() + "' already exists for this event"));
                    TicketType tt = new TicketType();
                    tt.setEventId(eventId);
                    tt.setName(req.name());
                    tt.setDescription(req.description());
                    tt.setColor(req.color());
                    tt.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
                    tt.setTotalCapacity(req.totalCapacity());
                    tt.setBasePrice(req.basePrice());
                    tt.setGroupTicket(req.groupTicket() != null ? req.groupTicket() : false);
                    tt.setGroupSize(req.groupSize());
                    tt.setActive(true);
                    tt.setHidden(false);
                    tt.setSalesStartAt(req.salesStartAt());
                    tt.setSalesEndAt(req.salesEndAt());
                    tt.setCreatedAt(OffsetDateTime.now());
                    tt.setUpdatedAt(OffsetDateTime.now());
                    return ticketTypeRepository.save(tt);
                })
                .map(TicketTypeResponse::from);
    }

    public Mono<TicketTypeResponse> update(UUID id, UpdateTicketTypeRequest req) {
        return ticketTypeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket type not found")))
                .flatMap(tt -> {
                    if (req.name() == null || req.name().equalsIgnoreCase(tt.getName()))
                        return Mono.just(tt);
                    return ticketTypeRepository.existsByEventIdAndNameIgnoreCaseAndIdNot(tt.getEventId(), req.name(), id)
                            .flatMap(taken -> taken
                                    ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
                                            "A ticket type named '" + req.name() + "' already exists for this event"))
                                    : Mono.just(tt));
                })
                .flatMap(tt -> {
                    if (req.name() != null) tt.setName(req.name());
                    if (req.description() != null) tt.setDescription(req.description());
                    if (req.color() != null) tt.setColor(req.color());
                    if (req.sortOrder() != null) tt.setSortOrder(req.sortOrder());
                    if (req.totalCapacity() != null) tt.setTotalCapacity(req.totalCapacity());
                    if (req.basePrice() != null) tt.setBasePrice(req.basePrice());
                    if (req.groupTicket() != null) tt.setGroupTicket(req.groupTicket());
                    if (req.groupSize() != null) tt.setGroupSize(req.groupSize());
                    if (req.active() != null) tt.setActive(req.active());
                    if (req.hidden() != null) tt.setHidden(req.hidden());
                    if (req.salesStartAt() != null) tt.setSalesStartAt(req.salesStartAt());
                    if (req.salesEndAt() != null) tt.setSalesEndAt(req.salesEndAt());
                    tt.setUpdatedAt(OffsetDateTime.now());
                    return ticketTypeRepository.save(tt);
                })
                .map(TicketTypeResponse::from);
    }

    public Mono<Void> delete(UUID eventId, UUID id) {
        return ticketTypeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket type not found")))
                .flatMap(tt -> {
                    if (!tt.getEventId().equals(eventId))
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket type not found"));
                    return ticketTypeRepository.delete(tt);
                });
    }
}
