package net.tylersoft.events.config;

import net.tylersoft.common.security.RoleClaimConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(ex -> {
                    ex.accessDeniedHandler((exchange, denied) -> {
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        var body = """
                                {"status":"01", "message":"You dont have required permissions to access this resource"}
                                """;
                        var buffer = exchange.getResponse().bufferFactory()
                                .wrap(body.getBytes(StandardCharsets.UTF_8));
                        return exchange.getResponse().writeWith(Mono.just(buffer));
                    });
                })
                .authorizeExchange(auth -> auth
                        .pathMatchers(
                                "/api/v2/events/{eventId}",
                                "/api/v2/events/merchant/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        .pathMatchers(
                                "/api/v2/purchases/**",
                                "/api/v2/purchases/tickets",
                                "/api/v1/events/**"
                        ).permitAll()
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new RoleClaimConverter())))
                .build();
    }
}
