package net.tylersoft.users.repository;

import net.tylersoft.users.model.IdentityDocument;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface IdentityDocumentRepository extends R2dbcRepository<IdentityDocument, UUID> {

    Flux<IdentityDocument> findByCustomerId(UUID customerId);
}
