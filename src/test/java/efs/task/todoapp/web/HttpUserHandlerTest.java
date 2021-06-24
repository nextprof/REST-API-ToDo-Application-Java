package efs.task.todoapp.web;

import com.google.gson.Gson;
import efs.task.todoapp.repository.UserEntity;
import efs.task.todoapp.util.ToDoServerExtension;
import static efs.task.todoapp.web.HttpCode.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ToDoServerExtension.class)
class HttpUserHandlerTest {

    private HttpClient httpClient;
    private static final String TODO_APP_PATH = "http://localhost:8080/todo/";
    private static final String responseCode = "Response status code";
    private static final Gson gson = new Gson();
    
    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();
    }

    @Test
    @Timeout(1)
    void savingUser_shouldReturn_CREATED_StatusCode() throws IOException, InterruptedException {
        //given
        String bodyUser = gson.toJson(new UserEntity("username","password"));

        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        //when
        var httpResponse = httpClient.send(httpRequest, ofString());

        //then
        assertThat(httpResponse.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
    }

    @Test
    @Timeout(1)
    void savingUser_shouldReturn_CONFLICT_StatusCode() throws IOException, InterruptedException {
        //given
        String bodyUser = gson.toJson(new UserEntity("username","password"));

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseUser2 = httpClient.send(httpRequestUser,ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseUser2.statusCode()).as(responseCode).isEqualTo(CONFLICT.getCode());
    }

    @ParameterizedTest(name = "{index}: username={0}, password={1}")
    @CsvSource({",password","username,",","})
    @Timeout(1)
    void saveUser_shouldReturnBad_RequestCode() throws IOException, InterruptedException {
        //given
        String bodyUser = gson.toJson(new UserEntity(null,"testPassword"));

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        //when
        HttpResponse<String> httpResponse = httpClient.send(httpRequestUser, ofString());

        //then
        assertThat(httpResponse.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
    }
}