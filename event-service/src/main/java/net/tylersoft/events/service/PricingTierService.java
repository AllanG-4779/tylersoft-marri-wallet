package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.events.dto.pricingtier.CreatePricingTierRequest;
import net.tylersoft.events.dto.pricingtier.PricingTierResponse;
import net.tylersoft.events.dto.pricingtier.UpdatePricingTierRequest;
import net.tylersoft.events.model.PricingTier;
import net.tylersoft.events.repository.PricingTierRepository;
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
public class PricingTierService {

    private final PricingTierRepository pricingTierRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public Flux<PricingTierResponse> listByTicketType(UUID ticketTypeId) {
        return pricingTierRepository.findAllByTicketTypeIdOrderBySortOrderAsc(ticketTypeId).map(PricingTierResponse::from);
    }

    public Mono<PricingTierResponse> getById(UUID id) {
        return pricingTierRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pricing tier not found")))
                .map(PricingTierResponse::from);
    }

    public Mono<PricingTierResponse> create(UUID ticketTypeId, CreatePricingTierRequest req) {
        return ticketTypeRepository.existsById(ticketTypeId)
                .flatMap(exists -> {
                    if (!exists) return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket type not found"));
                    if (!req.endsAt().isAfter(req.startsAt()))
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be after startsAt"));
                    PricingTier pt = new PricingTier();
                    pt.setTicketTypeId(ticketTypeId);
                    pt.setName(req.name());
                    pt.setPrice(req.price());
                    pt.setQuantity(req.quantity());
                    pt.setStartsAt(req.startsAt());
                    pt.setEndsAt(req.endsAt());
                    pt.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
                    pt.setCreatedAt(OffsetDateTime.now());
                    pt.setUpdatedAt(OffsetDateTime.now());
                    return pricingTierRepository.save(pt);
                })
                .map(PricingTierResponse::from);
    }

    public Mono<PricingTierResponse> update(UUID id, UpdatePricingTierRequest req) {
        return pricingTierRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pricing tier not found")))
                .flatMap(pt -> {
                    if (req.name() != null) pt.setName(req.name());
                    if (req.price() != null) pt.setPrice(req.price());
                    if (req.quantity() != null) pt.setQuantity(req.quantity());
                    if (req.startsAt() != null) pt.setStartsAt(req.startsAt());
                    if (req.endsAt() != null) pt.setEndsAt(req.endsAt());
                    if (req.sortOrder() != null) pt.setSortOrder(req.sortOrder());
                    pt.setUpdatedAt(OffsetDateTime.now());
                    return pricingTierRepository.save(pt);
                })
                .map(PricingTierResponse::from);
    }

    public Mono<Void> delete(UUID ticketTypeId, UUID id) {
        return pricingTierRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pricing tier not found")))
                .flatMap(pt -> {
                    if (!pt.getTicketTypeId().equals(ticketTypeId))
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pricing tier not found"));
                    return pricingTierRepository.delete(pt);
                });
    }
}
