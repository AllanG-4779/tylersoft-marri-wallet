package net.tylersoft.events.repository;

import net.tylersoft.events.model.PricingTier;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface PricingTierRepository extends R2dbcRepository<PricingTier, UUID> {

    Flux<PricingTier> findAllByTicketTypeIdOrderBySortOrderAsc(UUID ticketTypeId);
}
