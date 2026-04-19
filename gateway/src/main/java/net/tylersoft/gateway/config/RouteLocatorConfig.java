package net.tylersoft.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteLocatorConfig {

    @Value("${AUTH_SERVICE_URL:http://localhost:8093}")
    private String authServiceUrl;

    @Value("${USER_SERVICE_URL:http://localhost:8091}")
    private String userServiceUrl;

    @Value("${WALLET_SERVICE_URL:http://localhost:8090}")
    private String walletServiceUrl;

    @Value("${PAYMENT_SERVICE_URL:http://localhost:8092}")
    private String paymentServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth-service: login + JWK set (public)
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**", "/.well-known/jwks.json")
                        .uri(authServiceUrl))
                // User management
                .route("user-service", r -> r
                        .path("/api/v2/users/**")
                        .uri(userServiceUrl))
                // Wallet: accounts, transactions, top-up, config
                .route("wallet-service", r -> r
                        .path("/api/v2/accounts/**",
                              "/api/v2/transactions/**",
                              "/api/v2/topup/**",
                              "/api/v2/config/**")
                        .uri(walletServiceUrl))
                // Payment: billing, intercape, third-party
                .route("payment-service", r -> r
                        .path("/api/v1/billing/**",
                              "/api/v1/intercape/**",
                              "/api/v1/third-party/**")
                        .uri(paymentServiceUrl))
                .build();
    }
}
