package net.tylersoft.users.common;

import lombok.extern.slf4j.Slf4j;
import net.tylersoft.common.exception.exceptions.UnauthorizedException;
import net.tylersoft.common.http.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class UsersGlobalExceptionHandler {

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

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.just(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(TimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public Mono<ApiResponse<Void>> handleTimeout(TimeoutException ex) {
        return Mono.just(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex,
                                                        ServerWebExchange exchange) {
        HttpStatusCode status = ex.getStatusCode();
        exchange.getResponse().setStatusCode(status);
        String reason = ex.getReason() != null ? ex.getReason() : status.toString();
        return Mono.just(ApiResponse.error(reason));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return Mono.just(ApiResponse.error("You don't have the required permissions to access this resource"));
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return Mono.just(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return Mono.just(ApiResponse.error("An unexpected error occurred. Please try again."));
    }
}
