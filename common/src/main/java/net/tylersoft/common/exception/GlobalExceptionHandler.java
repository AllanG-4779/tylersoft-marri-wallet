package net.tylersoft.common.exception;

import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.exception.exceptions.UnauthorizedException;
import net.tylersoft.common.http.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Validation failures from {@code @Validated @RequestBody} — all field errors joined.
     */
    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ApiResponse<Void>> handleValidation(WebExchangeBindException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        return Mono.just(ApiResponse.error(message));
    }

    /**
     * Business rule violations — 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.just(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Timeout from downstream HTTP calls — 408.
     */
    @ExceptionHandler(TimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public Mono<ApiResponse<Void>> handleTimeout(TimeoutException ex) {
        return Mono.just(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Auth / access errors thrown via {@link ResponseStatusException} (401, 403, 404, etc.).
     * Preserves the intended HTTP status code rather than falling back to 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex,
                                                        org.springframework.web.server.ServerWebExchange exchange) {
        HttpStatusCode status = ex.getStatusCode();
        exchange.getResponse().setStatusCode(status);
        String reason = ex.getReason() != null ? ex.getReason() : status.toString();
        return Mono.just(ApiResponse.error(reason));
    }

    /**
     * Safety net — logs unexpected errors and returns a generic 500.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return Mono.just(ApiResponse.error("An unexpected error occurred. Please try again."));
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<ApiResponse<Void>> handleUnexpected(UnauthorizedException ex) {
        log.error("Unhandled exception", ex);
        return Mono.just(ApiResponse.error(ex.getMessage()));
    }
}
