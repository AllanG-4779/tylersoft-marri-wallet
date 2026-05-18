package net.tylersoft.wallet.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("authGuard")
public class AuthGuard {

    public Mono<Boolean> isCustomerOrIntegrator(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String role = jwt.getClaimAsString("role");
            return Mono.just("CUSTOMER".equals(role) || "INTEGRATOR".equals(role));
        }
        return Mono.just(false);
    }
}
