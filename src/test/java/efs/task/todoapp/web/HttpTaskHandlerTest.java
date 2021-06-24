package efs.task.todoapp.web;

import com.google.gson.Gson;
import efs.task.todoapp.repository.TaskEntity;
import efs.task.todoapp.repository.UserEntity;
import efs.task.todoapp.util.ToDoServerExtension;
import static efs.task.todoapp.web.HttpCode.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ToDoServerExtension.class)
class HttpTaskHandlerTest {

    private HttpClient httpClient;
    private static final String TODO_APP_PATH = "http://localhost:8080/todo/";
    private static final Gson gson = new Gson();
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final String auth = "auth";
    private static final String responseCode = "Response status code";

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();
    }

    @ParameterizedTest(name = "{index}: username={0}, password={1}")
    @CsvSource({"username,password","anotherUsername,anotherPassword","a,b"})
    @Timeout(1)
    void savingTask_shouldReturn_CREATED_StatusCode(String username,String password) throws IOException, InterruptedException {

        //given
        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("description","2021-06-30"));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
    }

    @ParameterizedTest(name = "{index}: username={0}, password={1}")
    @CsvSource({"invalid,password","username,invalid","aa,bb","a,b","invalid123,invalid123"})
    @Timeout(1)
    void savingTask_shouldReturn_UNAUTHORIZED_StatusCode(String username,String password) throws IOException, InterruptedException {

        //given
        String bodyUser = gson.toJson(new UserEntity("username","password"));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(UNAUTHORIZED.getCode());
    }

    @ParameterizedTest(name = "{index}: description={0}, due={1}, header={2}")
    @CsvSource({",2021-06-30,auth",",,auth","buy milk,2021-06-30,invalid","buy milk,no-valid-data,auth"})
    @Timeout(1)
    void savingTask_shouldReturn_BAD_REQUEST_StatusCode(String description,String due,String header) throws IOException, InterruptedException {

        //given
        String username="username",password="password";
        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity(description,due));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(header,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());
        var httpResponseTask = httpClient.send(httpRequestTask_POST, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
    }

    @ParameterizedTest(name = "{index}: headerUsername={0},headerPassword={1}")
    @CsvSource({",","invalidHeaderUsername,",",invalidHeaderPassword","invalidHeaderUsername,invalidHeaderPassword"})
    @Timeout(1)
    void savingTask_undecodableHeader_shouldReturn_BAD_REQUEST_StatusCode(String headerUsername, String headerPassword)
            throws IOException, InterruptedException {
        //given
        String bodyUser = gson.toJson(new UserEntity("username","password"));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","12:12:12"));

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,headerUsername+":"+headerPassword)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
    }


    @ParameterizedTest(name = "{index}: description={0},due={1}")
    @CsvSource({"buy milk,2021-06-30","buy milk,"})
    @Timeout(1)
    void gettingListOfTasks_shouldReturn_OK_StatusCode(String description,String due)
            throws IOException, InterruptedException {

        //given
        String username="username",password="password";
        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity(description,due));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        var httpRequestTask_GET = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .GET()
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask, ofString());
        var httpResponseTask_POST2 = httpClient.send(httpRequestTask, ofString());
        var httpResponseTask_GET = httpClient.send(httpRequestTask_GET, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST2.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_GET.statusCode()).as(responseCode).isEqualTo(OK.getCode());
    }


    @ParameterizedTest(name = "{index}: header={0}")
    @CsvSource({"invalidAuthHeader","anotherInvalidAuthHeader"})
    @Timeout(1)
    void gettingListOfTasks_shouldReturn_BAD_REQUEST_StatusCode(String header) throws IOException, InterruptedException {

        //given
        String username="username",password="password";
        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        var httpRequestTask_GET = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .GET()
                .header(header,token)
                .build();

        var httpRequestTask_GET2 = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .GET()
                .header(auth,"totallyInvalidUsername"+":"+"totallyInvalidPassword")
                .build();

        var httpRequestTask_GET3 = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .GET()
                .header(auth,"")
                .build();

        var httpRequestTask_GET4 = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .GET()
                .header(auth,":")
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());

        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST, ofString());
        var httpResponseTask_POST2 = httpClient.send(httpRequestTask_POST, ofString());

        var httpResponseTask_GET = httpClient.send(httpRequestTask_GET, ofString());
        var httpResponseTask_GET2 = httpClient.send(httpRequestTask_GET2, ofString());
        var httpResponseTask_GET3 = httpClient.send(httpRequestTask_GET3, ofString());
        var httpResponseTask_GET4 = httpClient.send(httpRequestTask_GET4, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST2.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_GET.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
        assertThat(httpResponseTask_GET2.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
        assertThat(httpResponseTask_GET3.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
        assertThat(httpResponseTask_GET4.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
    }

    @ParameterizedTest(name = "{index}: username={0}, password={1}")
    @CsvSource({"invalidUsername,password","username,invalidPassword","invalidUsername,invalidPassword"})
    @Timeout(1)
    void gettingListOfTasks_shouldReturn_UNAUTHORIZED_StatusCode(String badUsername,String badPassword)
            throws IOException, InterruptedException {
        //given
        String username="username",password="password";
        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));

        var token = getToken(username,password);
        var badToken = getToken(badUsername,badPassword);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        var httpRequestTask_GET = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .GET()
                .header(auth,badToken)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());

        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST, ofString());
        var httpResponseTask_POST2 = httpClient.send(httpRequestTask_POST, ofString());

        var httpResponseTask_GET = httpClient.send(httpRequestTask_GET, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST2.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_GET.statusCode()).as(responseCode).isEqualTo( UNAUTHORIZED.getCode() );
    }

    @ParameterizedTest(name = "{index}: description={0}, due={1}")
    @CsvSource({"buy milk,2021-06-30","buy milk,"})
    @Timeout(1)
    void gettingSpecifiedTask_shouldReturn_OK_StatusCode(String description,String due) throws IOException, InterruptedException {

        //given
        String username="username",password="password";
        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity(description,due));

        var token = getToken(username,password);
        
        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST, ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_GET = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .GET()
                .header(auth,token)
                .build();

        var httpResponseTask_GET = httpClient.send(httpRequestTask_GET, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_GET.statusCode()).as(responseCode).isEqualTo(OK.getCode());
    }


    @ParameterizedTest(name = "{index}: header={0}")
    @ValueSource(strings = {"invalidAuthHeader","anotherInvalidAuthHeader"})
    @Timeout(1)
    void gettingSpecifiedTask_shouldReturn_BAD_REQUEST_StatusCode(String header)
            throws IOException, InterruptedException {
        //given
        String username="username",password="password";
        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));

        var token = getToken(username,password);

        var encodedPassword = new String(encoder.encode(password.getBytes(StandardCharsets.UTF_8)));

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST, ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_GET = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .GET()
                .header(header,token)
                .build();

        var httpRequestTask_GET2 = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .GET()
                .header(auth,"invalidFormatToDecode"+":"+encodedPassword)
                .build();

        var httpResponseTask_GET = httpClient.send(httpRequestTask_GET, ofString());
        var httpResponseTask_GET2 = httpClient.send(httpRequestTask_GET2, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_GET.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
        assertThat(httpResponseTask_GET2.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
    }

    @ParameterizedTest(name = "{index}: username={0},password={1}")
    @CsvSource({"username,invalidPassword","invalidUsername,password","invalidUsername,invalidPassword"})
    @Timeout(1)
    void gettingSpecifiedTask_shouldReturn_UNAUTHORIZED_StatusCode(String badUsername,String badPassword)
            throws IOException, InterruptedException {
        //given
        String username="username",password="password";
        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));

        var token = getToken(username,password);

        String encodedBadUsername = new String(encoder.encode(badUsername.getBytes(StandardCharsets.UTF_8)));
        String encodedBadPassword = new String(encoder.encode(badPassword.getBytes(StandardCharsets.UTF_8)));

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST, ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_GET = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .GET()
                .header(auth,encodedBadUsername+":"+encodedBadPassword)
                .build();

        var httpResponseTask_GET = httpClient.send(httpRequestTask_GET, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_GET.statusCode()).as(responseCode).isEqualTo( UNAUTHORIZED.getCode() );
    }

    @Test
    @Timeout(1)
    void gettingSpecifiedTask_shouldReturn_FORBIDDEN_StatusCode() throws IOException, InterruptedException {

        //given
        String username="username",password="password";
        String username2="username2",password2="password2";

        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyUser2 = gson.toJson(new UserEntity(username2,password2));

        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));

        var token = getToken(username,password);
        var token2 = getToken(username2,password2);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestUser2 = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser2))
                .build();

        var httpRequestTask = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());
        var httpResponseUser2 = httpClient.send(httpRequestUser2, ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask, ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_GET = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .GET()
                .header(auth,token2)
                .build();

        HttpResponse<String> httpResponseTask_GET = httpClient.send(httpRequestTask_GET, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseUser2.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_GET.statusCode()).as(responseCode).isEqualTo(FORBIDDEN.getCode());
    }


    @ParameterizedTest(name = "{index}: invalidTaskID={0}")
    @CsvSource({"237e9877-e79b-12d4-a765-321741963000","237e9877-e75b-12c4-b765-321741924213"})
    @Timeout(1)
    void gettingSpecifiedTask_shouldReturn_NOT_FOUND_StatusCode(String invalidTaskID) throws IOException, InterruptedException {

        //given
        String username="name",password="password";

        String bodyUser = gson.toJson(new UserEntity(username,password));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_GET = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+invalidTaskID))
                .GET()
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser, ofString());
        var httpResponseTask_GET = httpClient.send(httpRequestTask_GET, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_GET.statusCode()).as(responseCode).isEqualTo(NOT_FOUND.getCode());
    }


    @ParameterizedTest(name = "{index}: description={0}, due={1}")
    @CsvSource({"new description,2021-07-02","new description,"})
    @Timeout(1)
    void updatingTask_shouldReturn_OK_StatusCode(String description,String due)
            throws IOException, InterruptedException {
        //given
        String username="username",password="password";

        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));
        String bodyUpdatedTask = gson.toJson(new TaskEntity(description,due));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST,ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_PUT = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .PUT(HttpRequest.BodyPublishers.ofString(bodyUpdatedTask))
                .header(auth,token)
                .build();

        var httpResponseTask_PUT = httpClient.send(httpRequestTask_PUT, ofString());
        var httpResponseTask_PUT2 = httpClient.send(httpRequestTask_PUT, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_PUT.statusCode()).as(responseCode).isEqualTo(OK.getCode());
        assertThat(httpResponseTask_PUT2.statusCode()).as(responseCode).isEqualTo(OK.getCode());
    }

    @ParameterizedTest(name = "{index}: description={0}, due={1}, header={2}")
    @CsvSource({"new description,no-valid-data,auth","new description,2021-07-02,badHeader",",2021-07-02,auth"})
    @Timeout(1)
    void updatingTask_shouldReturn_BAD_REQUEST_StatusCode(String description,String due,String header)
            throws IOException, InterruptedException {
        //given
        String username="username",password="password";

        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));
        String bodyUpdatedTask = gson.toJson(new TaskEntity(description,due));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST,ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_PUT = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .PUT(HttpRequest.BodyPublishers.ofString(bodyUpdatedTask))
                .header(header,token)
                .build();

        var httpResponseTask_PUT = httpClient.send(httpRequestTask_PUT, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_PUT.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
    }

    @ParameterizedTest(name = "{index}: username={0}, password={1}")
    @CsvSource({"username,invalidPassword","invalidUsername,password","invalidUsername,invalidPassword"})
    @Timeout(1)
    void updatingTask_shouldReturn_UNAUTHORIZED_StatusCode(String badUsername,String badPassword)
            throws IOException, InterruptedException {
        //given
        String username="username",password="password";

        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));
        String bodyUpdatedTask = gson.toJson(new TaskEntity("new description","2021-07-02"));

        var token = getToken(username,password);
        var badToken = getToken(badUsername,badPassword);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST,ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_PUT = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .PUT(HttpRequest.BodyPublishers.ofString(bodyUpdatedTask))
                .header(auth,badToken)
                .build();

        var httpResponseTask_PUT = httpClient.send(httpRequestTask_PUT, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_PUT.statusCode()).as(responseCode).isEqualTo( UNAUTHORIZED.getCode() );
    }

    @Test
    @Timeout(1)
    void updatingTask_shouldReturn_FORBIDDEN_StatusCode() throws IOException, InterruptedException {

        //given
        String username="username",password="password";
        String username2="username2",password2 = "password2";

        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyUser2 = gson.toJson(new UserEntity(username2,password2));

        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));
        String bodyUpdatedTask = gson.toJson(new TaskEntity("new description","2021-07-02"));

        var token = getToken(username,password);

        var token2 = getToken(username2,password2);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestUser2 = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser2))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseUser2 = httpClient.send(httpRequestUser2,ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST,ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_PUT = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .PUT(HttpRequest.BodyPublishers.ofString(bodyUpdatedTask))
                .header(auth,token2)
                .build();

        var httpResponseTask_PUT = httpClient.send(httpRequestTask_PUT, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseUser2.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_PUT.statusCode()).as(responseCode).isEqualTo(FORBIDDEN.getCode());
    }

    @ParameterizedTest(name = "{index}: invalidTaskID={0}")
    @CsvSource({"237e9877-e79b-12d4-a765-321741963000","237e9877-e75b-12c4-b765-321741924213"})
    @Timeout(1)
    void updatingTask_shouldReturn_NOT_FOUND_StatusCode(String invalidTaskID) throws IOException, InterruptedException {

        //given
        String username="username",password="password";

        String bodyUser = gson.toJson(new UserEntity(username,password));

        String bodyUpdatedTask = gson.toJson(new TaskEntity("new description","2021-07-02"));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_PUT = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+invalidTaskID))
                .PUT(HttpRequest.BodyPublishers.ofString(bodyUpdatedTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseTask_PUT = httpClient.send(httpRequestTask_PUT,ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_PUT.statusCode()).as(responseCode).isEqualTo(NOT_FOUND.getCode());
    }

    @Test
    @Timeout(1)
    void deletingTask_shouldReturn_OK_StatusCode() throws IOException, InterruptedException {

        //given
        String username="username",password="password";

        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("new description","2021-07-02"));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST,ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_DELETE = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .DELETE()
                .header(auth,token)
                .build();

        var httpResponseTask_DELETE = httpClient.send(httpRequestTask_DELETE, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_DELETE.statusCode()).as(responseCode).isEqualTo(OK.getCode());
    }

    @ParameterizedTest(name = "{index}: header={0}, id={1}")
    @CsvSource({"auth,invalidID","invalidHeader,237e9877-e79b-12d4-a765-321741963000","invalidHeader,invalidID"})
    @Timeout(1)
    void deletingTask_shouldReturn_BAD_REQUEST_StatusCode(String header,String id) throws IOException, InterruptedException {

        //given
        String username="username",password="password";

        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("newDescription","2021-07-02"));

        var token = getToken(username,password);
        
        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        var httpRequestTask_DELETE = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .DELETE()
                .header(header,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST,ofString());
        var httpResponseTask_DELETE = httpClient.send(httpRequestTask_DELETE, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_DELETE.statusCode()).as(responseCode).isEqualTo(BAD_REQUEST.getCode());
    }

    @ParameterizedTest(name = "{index}: username={0},password={1}")
    @CsvSource({"name,invalidPassword","invalidUsername,pass","invalidUsername,invalidPassword"})
    @Timeout(1)
    void deletingTask_shouldReturn_UNAUTHORIZED_StatusCode(String badUsername,String badPassword)
            throws IOException, InterruptedException {
        //given
        String username="name",password="pass";

        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("description","2021-06-30"));

        var token = getToken(username,password);
        var badToken = getToken(badUsername,badPassword);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST,ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_DELETE = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .DELETE()
                .header(auth,badToken)
                .build();

        var httpResponseTask_DELETE = httpClient.send(httpRequestTask_DELETE, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_DELETE.statusCode()).as(responseCode).isEqualTo( UNAUTHORIZED.getCode() );
    }


    @Test
    @Timeout(1)
    void deletingTask_shouldReturn_FORBIDDEN_StatusCode() throws IOException, InterruptedException {

        String username="name",password="password";
        String username2="name2",password2 = "password2";

        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyUser2 = gson.toJson(new UserEntity(username2,password2));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));

        var token = getToken(username,password);
        var token2 = getToken(username2,password2);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestUser2 = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser2))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseUser2 = httpClient.send(httpRequestUser2,ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST,ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_DELETE = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .DELETE()
                .header(auth,token2)
                .build();

        var httpResponseTask_DELETE = httpClient.send(httpRequestTask_DELETE, ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseUser2.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_POST.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_DELETE.statusCode()).as(responseCode).isEqualTo(FORBIDDEN.getCode());
    }


    @ParameterizedTest(name = "{index}: invalidTaskID={0}")
    @CsvSource({"237e9877-e79b-12d4-a765-321741963000","237e9877-e75b-12c4-b765-321741924213"})
    @Timeout(1)
    void deletingTask_shouldReturn_NOT_FOUND_StatusCode(String invalidTaskID)
            throws IOException, InterruptedException {
        //giveN
        String username="username",password="password";

        String bodyUser = gson.toJson(new UserEntity(username,password));

        var token = getToken(username,password);

        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_DELETE = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+invalidTaskID))
                .DELETE()
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseTask_DELETE = httpClient.send(httpRequestTask_DELETE,ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_DELETE.statusCode()).as(responseCode).isEqualTo(NOT_FOUND.getCode());
    }

    @Test
    @Timeout(1)
    void doubleDeletingTask_shouldReturn_NOT_FOUND_StatusCode()
            throws IOException, InterruptedException {
        //given
        String username="username",password="password";

        String bodyUser = gson.toJson(new UserEntity(username,password));
        String bodyTask = gson.toJson(new TaskEntity("buy milk","2021-06-30"));

        var token = getToken(username,password);
        
        var httpRequestUser = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "user"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyUser))
                .build();

        var httpRequestTask_POST = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task"))
                .POST(HttpRequest.BodyPublishers.ofString(bodyTask))
                .header(auth,token)
                .build();

        //when
        var httpResponseUser = httpClient.send(httpRequestUser,ofString());
        var httpResponseTask_POST = httpClient.send(httpRequestTask_POST,ofString());

        var properties = gson.fromJson(httpResponseTask_POST.body(), Properties.class);
        String id = properties.getProperty("id");

        var httpRequestTask_DELETE = HttpRequest.newBuilder()
                .uri(URI.create(TODO_APP_PATH + "task/"+id))
                .DELETE()
                .header(auth,token)
                .build();

        //when
        var httpResponseTask_DELETE = httpClient.send(httpRequestTask_DELETE,ofString());
        var httpResponseTask_DELETE2 = httpClient.send(httpRequestTask_DELETE,ofString());

        //then
        assertThat(httpResponseUser.statusCode()).as(responseCode).isEqualTo(CREATED.getCode());
        assertThat(httpResponseTask_DELETE.statusCode()).as(responseCode).isEqualTo(OK.getCode());
        assertThat(httpResponseTask_DELETE2.statusCode()).as(responseCode).isEqualTo(NOT_FOUND.getCode());
    }

    private String getToken(String username,String password){
        var encodedUsername = new String(encoder.encode(username.getBytes(StandardCharsets.UTF_8)));
        var encodedPassword = new String(encoder.encode(password.getBytes(StandardCharsets.UTF_8)));
        return encodedUsername+":"+encodedPassword;
    }

}