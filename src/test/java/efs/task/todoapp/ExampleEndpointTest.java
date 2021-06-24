package efs.task.todoapp;

import efs.task.todoapp.util.ToDoServerExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ToDoServerExtension.class)
class ExampleEndpointTest {

    public static final int NOT_FOUND = 404;
    public static final String TODO_APP_PATH = "http://localhost:8080/todo/";

    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();
    }

    @Test
    @Timeout(1)
    void shouldReturnNotFoundStatusForUnhandledPaths() throws IOException, InterruptedException {
        //given
        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "non/existing/endpoint"))
                .GET()
                .build();

        //when
        var httpResponse = httpClient.send(httpRequest,  ofString());

        //then
        assertThat(httpResponse.statusCode()).as("Response status code").isEqualTo(NOT_FOUND);
    }
}