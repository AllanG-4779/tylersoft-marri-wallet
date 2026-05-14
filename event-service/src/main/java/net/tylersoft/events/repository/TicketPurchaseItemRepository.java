package net.tylersoft.events.repository;

import net.tylersoft.events.model.TicketPurchaseItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface TicketPurchaseItemRepository extends R2dbcRepository<TicketPurchaseItem, UUID> {

    Flux<TicketPurchaseItem> findAllByPurchaseId(UUID purchaseId);
}
