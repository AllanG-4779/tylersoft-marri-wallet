package net.tylersoft.wallet.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("customerGuard")
public class CustomerGuard {

    public Mono<Boolean> isCustomer(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return Mono.just("CUSTOMER".equals(jwt.getClaimAsString("role")));
        }
        return Mono.just(false);
    }
}
