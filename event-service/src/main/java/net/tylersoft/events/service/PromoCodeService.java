package net.tylersoft.events.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.events.dto.promocode.CreatePromoCodeRequest;
import net.tylersoft.events.dto.promocode.PromoCodeResponse;
import net.tylersoft.events.dto.promocode.UpdatePromoCodeRequest;
import net.tylersoft.events.model.PromoCode;
import net.tylersoft.events.repository.EventRepository;
import net.tylersoft.events.repository.PromoCodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final EventRepository eventRepository;

    public Flux<PromoCodeResponse> listByEvent(UUID eventId) {
        return promoCodeRepository.findAllByEventIdOrderByCreatedAtDesc(eventId).map(PromoCodeResponse::from);
    }

    public Mono<PromoCodeResponse> getById(UUID eventId, UUID id) {
        return promoCodeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found")))
                .flatMap(pc -> {
                    if (!pc.getEventId().equals(eventId))
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found"));
                    return Mono.just(pc);
                })
                .map(PromoCodeResponse::from);
    }

    public Mono<PromoCodeResponse> create(UUID eventId, CreatePromoCodeRequest req) {
        return eventRepository.existsById(eventId)
                .flatMap(exists -> {
                    if (!exists) return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
                    return promoCodeRepository.existsByEventIdAndCode(eventId, req.code());
                })
                .flatMap(codeExists -> {
                    if (codeExists) return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Promo code already exists for this event"));
                    PromoCode pc = new PromoCode();
                    pc.setEventId(eventId);
                    pc.setCode(req.code().toUpperCase());
                    pc.setDescription(req.description());
                    pc.setDiscountType(req.discountType());
                    pc.setDiscountValue(req.discountValue());
                    pc.setMaxUses(req.maxUses());
                    pc.setMinOrderAmount(req.minOrderAmount());
                    pc.setStartsAt(req.startsAt());
                    pc.setExpiresAt(req.expiresAt());
                    pc.setActive(true);
                    pc.setCreatedAt(OffsetDateTime.now());
                    pc.setUpdatedAt(OffsetDateTime.now());
                    return promoCodeRepository.save(pc);
                })
                .map(PromoCodeResponse::from);
    }

    public Mono<PromoCodeResponse> update(UUID eventId, UUID id, UpdatePromoCodeRequest req) {
        return promoCodeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found")))
                .flatMap(pc -> {
                    if (!pc.getEventId().equals(eventId))
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found"));
                    if (req.description() != null) pc.setDescription(req.description());
                    if (req.discountValue() != null) pc.setDiscountValue(req.discountValue());
                    if (req.maxUses() != null) pc.setMaxUses(req.maxUses());
                    if (req.minOrderAmount() != null) pc.setMinOrderAmount(req.minOrderAmount());
                    if (req.startsAt() != null) pc.setStartsAt(req.startsAt());
                    if (req.expiresAt() != null) pc.setExpiresAt(req.expiresAt());
                    if (req.active() != null) pc.setActive(req.active());
                    pc.setUpdatedAt(OffsetDateTime.now());
                    return promoCodeRepository.save(pc);
                })
                .map(PromoCodeResponse::from);
    }

    public Mono<Void> delete(UUID eventId, UUID id) {
        return promoCodeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found")))
                .flatMap(pc -> {
                    if (!pc.getEventId().equals(eventId))
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found"));
                    return promoCodeRepository.delete(pc);
                });
    }
}
