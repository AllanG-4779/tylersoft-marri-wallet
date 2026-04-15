package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.model.GlServiceMapping;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GlServiceMappingRepository extends R2dbcRepository<GlServiceMapping, Integer> {

    Mono<GlServiceMapping> findByAccountNumber(String accountNumber);

    Flux<GlServiceMapping> findByServiceId(Integer serviceId);
}
