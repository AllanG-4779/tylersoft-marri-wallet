package net.tylersoft.wallet.repository;

import net.tylersoft.wallet.model.Channel;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface ChannelRepository extends R2dbcRepository<Channel, Integer> {

    Mono<Channel> findByChannelName(String channelName);

    Mono<Channel> findByClientId(String clientId);
}
