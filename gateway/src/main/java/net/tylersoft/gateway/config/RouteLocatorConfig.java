package net.tylersoft.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteLocatorConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth-service: login + JWK set (public)
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**", "/.well-known/jwks.json")
                        .uri("lb://auth-service"))
                // User management
                .route("user-service", r -> r
                        .path("/api/v2/users/**")
                        .uri("lb://wallet-user-service"))
                // Wallet: accounts, transactions, top-up, config
                .route("wallet-service", r -> r
                        .path("/api/v2/accounts/**",
                              "/api/v2/transactions/**",
                              "/api/v2/topup/**",
                              "/api/v2/airtime/**",
                              "/api/v2/config/**")
                        .uri("lb://wallet-service"))
                // Payment: billing, intercape, third-party
                .route("payment-service", r -> r
                        .path("/api/v1/billing/**",
                              "/api/v1/intercape/**",
                              "/api/v1/card/**",
                              "/api/v1/third-party/**")
                        .uri("lb://payment-service"))
                .build();
    }
}
