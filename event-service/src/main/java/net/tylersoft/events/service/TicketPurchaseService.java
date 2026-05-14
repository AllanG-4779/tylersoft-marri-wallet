package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.common.http.dto.Page;
import net.tylersoft.events.common.EventStatus;
import net.tylersoft.events.common.PurchaseStatus;
import net.tylersoft.events.common.TicketClassStatus;
import net.tylersoft.events.common.TicketStatus;
import net.tylersoft.events.dto.purchase.*;
import net.tylersoft.events.model.EventTicket;
import net.tylersoft.events.model.EventTicketClass;
import net.tylersoft.events.model.TicketPurchase;
import net.tylersoft.events.model.TicketPurchaseItem;
import net.tylersoft.events.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketPurchaseService {

    private final TicketPurchaseRepository purchaseRepository;
    private final TicketPurchaseItemRepository purchaseItemRepository;
    private final EventTicketClassRepository ticketClassRepository;
    private final EventTicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final TransactionalOperator transactionalOperator;
    private final DatabaseClient databaseClient;

    public Mono<PurchaseResponse> initiate(UUID customerId, CreatePurchaseRequest req) {
        return eventRepository.findById(req.eventId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")))
                .flatMap(event -> {
                    if (!EventStatus.PUBLISHED.name().equals(event.getStatus())
                            && !EventStatus.ONGOING.name().equals(event.getStatus()))
                        return Mono.error(new IllegalArgumentException("Event is not open for ticket sales"));

                    List<UUID> classIds = req.items().stream()
                            .map(PurchaseItemRequest::ticketClassId).toList();

                    return ticketClassRepository.findAllById(classIds)
                            .collectMap(EventTicketClass::getId)
                            .flatMap(classMap -> {
                                for (PurchaseItemRequest item : req.items()) {
                                    EventTicketClass tc = classMap.get(item.ticketClassId());
                                    if (tc == null)
                                        return Mono.error(new IllegalArgumentException("Ticket class not found: " + item.ticketClassId()));
                                    if (!tc.getEventId().equals(req.eventId()))
                                        return Mono.error(new IllegalArgumentException("Ticket class does not belong to this event: " + tc.getName()));
                                    if (!TicketClassStatus.ACTIVE.name().equals(tc.getStatus()))
                                        return Mono.error(new IllegalArgumentException("Ticket class is not available: " + tc.getName()));
                                    if ((tc.getCapacity() - tc.getSoldCount()) < item.quantity())
                                        return Mono.error(new IllegalArgumentException("Insufficient capacity for: " + tc.getName()));
                                }

                                BigDecimal total = req.items().stream()
                                        .map(i -> classMap.get(i.ticketClassId()).getPrice()
                                                .multiply(BigDecimal.valueOf(i.quantity())))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                String currency = classMap.values().iterator().next().getCurrency();

                                TicketPurchase purchase = new TicketPurchase();
                                purchase.setEventId(req.eventId());
                                purchase.setCustomerId(customerId);
                                purchase.setTotalAmount(total);
                                purchase.setCurrency(currency);
                                purchase.setStatus(PurchaseStatus.PENDING.name());
                                purchase.setStatusChangedAt(OffsetDateTime.now());
                                purchase.setCreatedAt(OffsetDateTime.now());
                                purchase.setUpdatedAt(OffsetDateTime.now());

                                return purchaseRepository.save(purchase)
                                        .flatMap(saved -> {
                                            List<TicketPurchaseItem> items = req.items().stream().map(itemReq -> {
                                                EventTicketClass tc = classMap.get(itemReq.ticketClassId());
                                                TicketPurchaseItem item = new TicketPurchaseItem();
                                                item.setPurchaseId(saved.getId());
                                                item.setTicketClassId(itemReq.ticketClassId());
                                                item.setQuantity(itemReq.quantity());
                                                item.setUnitPrice(tc.getPrice());
                                                item.setSubtotal(tc.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
                                                item.setCreatedAt(OffsetDateTime.now());
                                                return item;
                                            }).collect(Collectors.toList());

                                            return purchaseItemRepository.saveAll(items).collectList()
                                                    .map(savedItems -> buildResponse(saved, savedItems, classMap));
                                        });
                            });
                })
                .as(transactionalOperator::transactional);
    }

    public Mono<PurchaseResponse> confirm(UUID purchaseId, String paymentRef) {
        return purchaseRepository.findById(purchaseId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found")))
                .flatMap(purchase -> {
                    if (!PurchaseStatus.PENDING.name().equals(purchase.getStatus()))
                        return Mono.error(new IllegalArgumentException("Purchase is not in PENDING status"));

                    purchase.setPaymentReference(paymentRef);
                    purchase.setStatus(PurchaseStatus.CONFIRMED.name());
                    purchase.setStatusChangedAt(OffsetDateTime.now());
                    purchase.setUpdatedAt(OffsetDateTime.now());

                    return purchaseRepository.save(purchase)
                            .flatMap(saved -> purchaseItemRepository.findAllByPurchaseId(saved.getId())
                                    .flatMap(item -> issueTickets(item, saved))
                                    .then(buildPurchaseResponse(saved)));
                })
                .as(transactionalOperator::transactional);
    }

    public Mono<PurchaseResponse> cancel(UUID purchaseId, UUID customerId) {
        return purchaseRepository.findByIdAndCustomerId(purchaseId, customerId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found")))
                .flatMap(purchase -> {
                    if (!PurchaseStatus.PENDING.name().equals(purchase.getStatus()))
                        return Mono.error(new IllegalArgumentException("Only PENDING purchases can be cancelled"));

                    purchase.setStatus(PurchaseStatus.CANCELLED.name());
                    purchase.setStatusChangedAt(OffsetDateTime.now());
                    purchase.setUpdatedAt(OffsetDateTime.now());
                    return purchaseRepository.save(purchase);
                })
                .flatMap(this::buildPurchaseResponse);
    }

    public Mono<Page<PurchaseResponse>> getMyPurchases(UUID customerId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Mono.zip(
                purchaseRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                        .flatMap(this::buildPurchaseResponse).collectList(),
                purchaseRepository.countByCustomerId(customerId)
        ).map(t -> Page.of(t.getT1(), page, size, t.getT2()));
    }

    public Mono<PurchaseResponse> getPurchase(UUID purchaseId, UUID customerId) {
        return purchaseRepository.findByIdAndCustomerId(purchaseId, customerId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found")))
                .flatMap(this::buildPurchaseResponse);
    }

    public Flux<EventTicketResponse> getMyTickets(UUID customerId) {
        return ticketRepository.findAllByCustomerIdOrderByIssuedAtDesc(customerId)
                .map(EventTicketResponse::from);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private Mono<Void> issueTickets(TicketPurchaseItem item, TicketPurchase purchase) {
        List<EventTicket> tickets = new ArrayList<>();
        for (int i = 0; i < item.getQuantity(); i++) {
            EventTicket ticket = new EventTicket();
            ticket.setPurchaseItemId(item.getId());
            ticket.setEventId(purchase.getEventId());
            ticket.setTicketClassId(item.getTicketClassId());
            ticket.setCustomerId(purchase.getCustomerId());
            ticket.setTicketCode(generateTicketCode());
            ticket.setStatus(TicketStatus.ISSUED.name());
            ticket.setIssuedAt(OffsetDateTime.now());
            ticket.setCreatedAt(OffsetDateTime.now());
            ticket.setUpdatedAt(OffsetDateTime.now());
            tickets.add(ticket);
        }
        return ticketRepository.saveAll(tickets).then()
                .then(incrementSoldCount(item.getTicketClassId(), item.getQuantity()));
    }

    private Mono<Void> incrementSoldCount(UUID ticketClassId, int quantity) {
        return databaseClient
                .sql("UPDATE events.event_ticket_classes SET sold_count = sold_count + :qty, updated_at = NOW() WHERE id = :id")
                .bind("qty", quantity)
                .bind("id", ticketClassId)
                .fetch().rowsUpdated().then();
    }

    private Mono<PurchaseResponse> buildPurchaseResponse(TicketPurchase purchase) {
        return purchaseItemRepository.findAllByPurchaseId(purchase.getId())
                .collectList()
                .flatMap(items -> {
                    List<UUID> classIds = items.stream().map(TicketPurchaseItem::getTicketClassId).toList();
                    return ticketClassRepository.findAllById(classIds)
                            .collectMap(EventTicketClass::getId)
                            .map(classMap -> buildResponse(purchase, items, classMap));
                });
    }

    private PurchaseResponse buildResponse(TicketPurchase purchase,
                                           List<TicketPurchaseItem> items,
                                           Map<UUID, EventTicketClass> classMap) {
        List<PurchaseItemResponse> itemResponses = items.stream()
                .map(item -> {
                    EventTicketClass tc = classMap.get(item.getTicketClassId());
                    return new PurchaseItemResponse(
                            item.getId(),
                            item.getTicketClassId(),
                            tc != null ? tc.getName() : null,
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getSubtotal()
                    );
                }).toList();

        return new PurchaseResponse(
                purchase.getId(),
                purchase.getEventId(),
                purchase.getCustomerId(),
                purchase.getTotalAmount(),
                purchase.getCurrency(),
                purchase.getStatus(),
                purchase.getPaymentReference(),
                itemResponses,
                purchase.getCreatedAt()
        );
    }

    private static String generateTicketCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
