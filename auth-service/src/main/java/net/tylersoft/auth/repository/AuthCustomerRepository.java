package net.tylersoft.auth.repository;

import net.tylersoft.auth.model.AuthCustomer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AuthCustomerRepository extends ReactiveCrudRepository<AuthCustomer, UUID> {

    Mono<AuthCustomer> findByPhoneNumber(String phoneNumber);
}
