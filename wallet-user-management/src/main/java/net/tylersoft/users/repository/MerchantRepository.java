package net.tylersoft.users.repository;

import net.tylersoft.users.model.Merchant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface MerchantRepository extends ReactiveCrudRepository<Merchant, UUID> {

    Mono<Boolean> existsByBusinessEmail(String businessEmail);
    Mono<Boolean> existsByBusinessPhone(String businessPhone);
    Mono<Merchant> findByMerchantCode(String merchantCode);


    Flux<Merchant> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Mono<Long> count();

    Flux<Merchant> findAllByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Mono<Long> countByStatus(String status);
}
