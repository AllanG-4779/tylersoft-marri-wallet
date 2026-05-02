package net.tylersoft.users.repository;

import net.tylersoft.users.model.MerchantDocument;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface MerchantDocumentRepository extends ReactiveCrudRepository<MerchantDocument, UUID> {

    Flux<MerchantDocument> findAllByMerchantId(UUID merchantId);
}
