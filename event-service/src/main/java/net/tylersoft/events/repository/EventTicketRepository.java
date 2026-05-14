package net.tylersoft.events.repository;

import net.tylersoft.events.model.EventTicket;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EventTicketRepository extends R2dbcRepository<EventTicket, UUID> {

    Mono<EventTicket> findByTicketCode(String ticketCode);

    Flux<EventTicket> findAllByPurchaseItemId(UUID purchaseItemId);

    Flux<EventTicket> findAllByCustomerIdOrderByIssuedAtDesc(UUID customerId);

    Flux<EventTicket> findAllByEventId(UUID eventId);
}
