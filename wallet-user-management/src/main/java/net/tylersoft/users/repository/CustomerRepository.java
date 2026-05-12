package net.tylersoft.users.repository;

import net.tylersoft.users.model.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerRepository extends R2dbcRepository<Customer, UUID> {

    Mono<Customer> findByPhoneNumber(String phoneNumber);

    Mono<Customer> findByEmail(String email);

    Mono<Boolean> existsByPhoneNumber(String phoneNumber);

    Mono<Boolean> existsByEmail(String email);

    Flux<Customer> findAllBy(Pageable pageable);

    Flux<Customer> findAllByStatus(String status, Pageable pageable);

    Mono<Long> countByStatus(String status);
}
