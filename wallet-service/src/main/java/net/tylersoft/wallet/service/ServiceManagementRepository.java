package net.tylersoft.wallet.service;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ServiceManagementRepository extends R2dbcRepository<ServiceManagement, Integer> {

    Mono<ServiceManagement> findByServiceCode(String serviceCode);

    Flux<ServiceManagement> findByServiceId(Integer serviceId);

    Flux<ServiceManagement> findByChannelId(Integer channelId);
}
