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
                // Auth-service: login, JWK set (public) + system-admin/role management (SYSTEM_ADMIN)
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**",
                              "/.well-known/jwks.json",
                              "/api/v1/admin/system-admins/**",
                              "/api/v1/admin/roles/**")
                        .uri("lb://auth-service"))
                // User management
                .route("user-service", r -> r
                        .path("/api/v2/users/**")
                        .uri("lb://wallet-user-service"))
                // Merchant + customer admin (wallet-user-management)
                .route("merchant-service", r -> r
                        .path("/api/v2/merchants/**",
                              "/api/v1/admin/merchants/**",
                              "/api/v1/admin/customers/**")
                        .uri("lb://wallet-user-service"))
                // Wallet: accounts, transactions, top-up, config, OTT redemption, admin transaction view, integrator transfer
                .route("wallet-service", r -> r
                        .path("/api/v2/accounts/**",
                              "/api/v2/transactions/**",
                              "/api/v2/topup/**",
                              "/api/v2/airtime/**",
                              "/api/v2/config/**",
                              "/api/v1/ott/redeem",
                              "/api/v1/admin/transactions/**",
                              "/api/v1/integrator/transfer/**")
                        .uri("lb://wallet-service"))
                // Payment: billing, intercape, card, OTT, third-party
                .route("payment-service", r -> r
                        .path("/api/v1/billing/**",
                              "/api/v1/intercape/**",
                              "/api/v1/card/**",
                              "/api/v1/ott/**",
                              "/api/v1/third-party/**")
                        .uri("lb://payment-service"))
                .build();
    }
}
