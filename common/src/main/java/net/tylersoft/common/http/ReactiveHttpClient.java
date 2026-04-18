package net.tylersoft.common.http;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReactiveHttpClient {

    private final WebClient webClient;

    private  <T, R> Mono<R> sendRequest(
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

        return responseSpec
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(
                                        new RuntimeException("Client Error: " + error)
                                ))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(
                                        new RuntimeException("Server Error: " + error)
                                ))
                )
                .bodyToMono(responseType);
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

