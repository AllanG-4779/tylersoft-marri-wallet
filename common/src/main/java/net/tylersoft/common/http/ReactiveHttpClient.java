package net.tylersoft.common.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveHttpClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${http.client.timeout-seconds:10}")
    private long timeoutSeconds;

    private <T, R> Mono<R> sendRequest(
            String url,
            HttpMethod method,
            Map<String, String> headers,
            T requestBody,
            Class<R> responseType
    ) {
        WebClient.RequestBodySpec requestSpec = webClient
                .method(method)
                .uri(url);

        // Add headers
        if (headers != null) {
            headers.forEach(requestSpec::header);
        }

        WebClient.ResponseSpec responseSpec;

        // Attach body if present
        if (requestBody != null) {
            responseSpec = requestSpec
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve();
        } else {
            responseSpec = requestSpec.retrieve();
        }
        log.info("Sending {} request to {} with headers: {} and body: {}", method, url, headers, objectMapper.writeValueAsString(requestBody));

        return responseSpec
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(
                                        new RuntimeException("Client Error: " + error)
                                ))
                )

                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(
                                        new RuntimeException("Server Error: " + error)
                                ))
                )
                .bodyToMono(String.class)

                .doOnSuccess(response -> {
                    // Log successful response if needed
                    log.info("Received response from {}: {}", url, response);
                })
                .flatMap(response -> {
                    try {
                        R result = objectMapper.readValue(response, responseType);
                        return Mono.just(result);
                    } catch (Exception e) {
                        log.error("Failed to parse response from {}: {}", url, response, e);
                        return Mono.error(new RuntimeException("Failed to parse response: " + e.getMessage()));
                    }
                })
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .onErrorMap(
                        java.util.concurrent.TimeoutException.class,
                        ex -> new TimeoutException("Request timed out after " + timeoutSeconds + "s: " + ex.getMessage())
                );
    }

    public <T> Mono<String> postRaw(String url, T body) {
        WebClient.RequestBodySpec requestSpec = webClient.method(HttpMethod.POST).uri(url);
        WebClient.ResponseSpec responseSpec = requestSpec
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve();
        log.info("Sending POST request to {} with body: {}", url, objectMapper.writeValueAsString(body));
        return responseSpec
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException("Client Error: " + error))))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException("Server Error: " + error))))
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Received response from {}: {}", url, response))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .onErrorMap(java.util.concurrent.TimeoutException.class,
                        ex -> new TimeoutException("Request timed out after " + timeoutSeconds + "s: " + ex.getMessage()));
    }

    public <T, R> Mono<R> post(String url, T body, Class<R> responseType) {
        return sendRequest(url, HttpMethod.POST, null, body, responseType);
    }

    public <T, R> Mono<R> post(String url, Map<String, String> headers, T body, Class<R> responseType) {
        return sendRequest(url, HttpMethod.POST, headers, body, responseType);
    }

    public <R> Mono<R> get(String url, Class<R> responseType) {
        return sendRequest(url, HttpMethod.GET, null, null, responseType);
    }

    public <R> Mono<R> get(String url, Map<String, String> headers, Class<R> responseType) {
        return sendRequest(url, HttpMethod.GET, headers, null, responseType);
    }
}

