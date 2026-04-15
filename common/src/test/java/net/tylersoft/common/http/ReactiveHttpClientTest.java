package net.tylersoft.common.http;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ReactiveHttpClientTest {

    @Test
    void testGet() {
        var webclient = WebClient.builder().build();
        ReactiveHttpClient client = new ReactiveHttpClient(webclient);
        String url = "https://jsonplaceholder.typicode.com/posts/1";
        String response = client.get(url, String.class).block();
        log.info(response);
        assertNotNull(response);
        assertTrue(response.contains("\"id\": 1"));
    }

}