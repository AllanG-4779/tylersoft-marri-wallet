package net.tylersoft.common.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Mono<String> getCurrentUserId() {
        return getJwt().map(Jwt::getSubject);
    }

    public static Mono<String> getCurrentUserPhone() {
        return getJwt().map(jwt -> jwt.getClaimAsString("phone"));
    }

    public static Mono<String> getCurrentUserRole() {
        return getJwt().map(jwt -> jwt.getClaimAsString("role"));
    }

    public static Mono<String> getCurrentDeviceId() {
        return getJwt().map(jwt -> jwt.getClaimAsString("deviceId"));
    }

    public static Mono<Jwt> getJwt() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth != null && auth.getPrincipal() instanceof Jwt)
                .map(auth -> (Jwt) auth.getPrincipal());
    }
}
