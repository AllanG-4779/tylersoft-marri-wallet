package net.tylersoft.common.http;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class WebClientConfig {

    /**
     * Jackson 3.x mapper — used by ReactiveHttpClient for HTTP response deserialization.
     * Coercion config handles Intercape's Content:"" empty-string responses.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .withCoercionConfig(LogicalType.POJO, cfg ->
                        cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .withCoercionConfig(LogicalType.Array, cfg ->
                        cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .build();
    }



    /**
     * Jackson 2.x mapper — required by the logging infrastructure (SensitiveJacksonModule etc.)
     * which is built on com.fasterxml.jackson and cannot be changed without a large refactor.
     */
    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper legacyObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }

    @Bean
    public WebClient webClient() throws Exception {
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(sslSpec -> sslSpec.sslContext(sslContext));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
