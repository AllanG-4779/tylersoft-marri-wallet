package net.tylersoft.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteLocatorConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/v2/auth/**")
                        .uri("lb://auth-service"))
                .route("wallet-service", r -> r.path("/api/v2/wallet/**")
                        .uri("lb://wallet-service"))
                .route("payment-service", r -> r.path("/api/v2/payment/**")
                        .uri("lb://payment-service"))
                .route("user-service", r -> r.path("/api/v2/user/**")
                        .uri("lb://user-service"))
                .route("fallback-route", r -> r.path("/**")
                        .uri("http://localhost:8080/fallback"))
                .build();
    }
}
