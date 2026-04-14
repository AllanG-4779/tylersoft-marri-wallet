package net.tylersoft.wallet.account;

import net.tylersoft.wallet.common.ApiResponse;
import net.tylersoft.wallet.common.CallerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountOpeningService accountOpeningService;

    /**
     * POST /api/v2/accounts — open a new wallet account.
     *
     * <h3>Caller types</h3>
     * <b>Wallet holder</b> (authenticated user):
     * <pre>
     *   Authorization: Bearer &lt;jwt&gt;
     *   X-User-Phone: 254700000000       ← populated by the API gateway after JWT validation
     *   X-User-Id:    u-abc123
     * </pre>
     * The phone number is sourced from the token; any {@code phoneNumber} in the
     * request body is ignored so the holder cannot open accounts for other people.
     *
     * <br><b>Third-party integrator</b>:
     * <pre>
     *   X-Api-Key:   &lt;api-key&gt;
     *   X-Client-Id: &lt;client-id&gt;
     * </pre>
     * The integrator supplies {@code phoneNumber} in the request body.
     */
    @PostMapping
    public Mono<ApiResponse<OpenAccountResult>> openAccount(
            @RequestBody OpenAccountRequest request,
            ServerWebExchange exchange) {

        return resolveCallerContext(exchange, request)
                .flatMap(caller -> accountOpeningService.openAccount(
                        caller.identity(),
                        request.currency(),
                        request.accountPrefix(),
                        caller.phoneNumber(),       // wallet holder → from token; third-party → from body
                        request.accountName(),
                        "create",
                        request.openingBalance()))
                .map(result -> result.isSuccess()
                        ? ApiResponse.ok(result)
                        : ApiResponse.error(result.statusCode() + " - " + result.message()));
    }

    // -------------------------------------------------------------------------
    // Caller resolution
    // -------------------------------------------------------------------------

    /**
     * Determines who is calling and builds the appropriate {@link CallerContext}.
     *
     * <p>Auth header precedence:
     * <ol>
     *   <li>{@code Authorization: Bearer <token>} → WALLET_HOLDER
     *   <li>{@code X-Api-Key} + {@code X-Client-Id} → THIRD_PARTY
     *   <li>Neither → 401 Unauthorized
     * </ol>
     *
     * <p>In production the Bearer token is validated upstream (Spring Security /
     * API gateway).  Here the resolved claims arrive as trusted gateway headers
     * ({@code X-User-Id}, {@code X-User-Phone}).
     */
    private static Mono<CallerContext> resolveCallerContext(
            ServerWebExchange exchange, OpenAccountRequest request) {

        HttpHeaders headers = exchange.getRequest().getHeaders();

        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            // ── Wallet holder ──────────────────────────────────────────────
            // Gateway strips/validates the JWT and forwards these trusted headers.
            String userId    = headers.getFirst("X-User-Id");
            String userPhone = headers.getFirst("X-User-Phone");

            if (userId == null || userPhone == null) {
                return Mono.error(new UnauthorizedException(
                        "Bearer token present but gateway headers X-User-Id / X-User-Phone are missing"));
            }
            return Mono.just(CallerContext.walletHolder(userId, userPhone));
        }

        String apiKey   = headers.getFirst("X-Api-Key");
        String clientId = headers.getFirst("X-Client-Id");
        if (apiKey != null && clientId != null) {
            // ── Third-party integrator ─────────────────────────────────────
            // API-key validation is handled by a WebFilter / gateway policy.
            // The phone number must be in the request body.
            if (request.phoneNumber() == null || request.phoneNumber().isBlank()) {
                return Mono.error(new UnauthorizedException(
                        "Third-party requests must include phoneNumber in the request body"));
            }
            return Mono.just(CallerContext.thirdParty(clientId, request.phoneNumber()));
        }

        return Mono.error(new UnauthorizedException(
                "Unauthorized: provide 'Authorization: Bearer <token>' or 'X-Api-Key' + 'X-Client-Id' headers"));
    }

    // -------------------------------------------------------------------------
    // Local exception — mapped to 401 by @ResponseStatus
    // -------------------------------------------------------------------------

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class UnauthorizedException extends RuntimeException {
        UnauthorizedException(String message) { super(message); }
    }
}
