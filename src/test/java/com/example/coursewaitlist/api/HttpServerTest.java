package com.example.coursewaitlist.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"course.capacity=1", "course.eligible-learner-ids=a"})
class HttpServerTest {
    @LocalServerPort
    private int port;

    @Test
    void startsAndAcceptsARealHttpRequest() throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + port + "/api/v1/offerings/CS101-FALL/registrations");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"learnerId\":\"a\"}")).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(201, response.statusCode());
            assertTrue(response.body().contains("\"status\":\"ENROLLED\""));
        }
    }
}
