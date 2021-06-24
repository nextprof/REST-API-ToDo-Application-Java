package efs.task.todoapp.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import efs.task.todoapp.repository.TaskEntity;
import efs.task.todoapp.service.Exceptions.BadRequestException;
import efs.task.todoapp.service.Exceptions.ForbiddenException;
import efs.task.todoapp.service.Exceptions.NotFoundException;
import efs.task.todoapp.service.Exceptions.UnauthorizedException;
import efs.task.todoapp.service.ToDoService;
import static efs.task.todoapp.web.HttpCode.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class HttpTaskHandler implements HttpHandler {

    private final ToDoService service;

    private static final Logger LOGGER = Logger.getLogger(HttpTaskHandler.class.getName());
    private static final Base64.Decoder decoder = Base64.getDecoder();
    private static final Gson gson = new Gson();
    private static final String uuidPattern = "/todo/task/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
    private static final String base64Pattern = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})$";

    public HttpTaskHandler(ToDoService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            var uri = exchange.getRequestURI().toString();
            var method = exchange.getRequestMethod();
            var auth = exchange.getRequestHeaders().getFirst("auth");
            var userData = validateAuthHeader(auth);
            var body = new String(exchange.getRequestBody().readAllBytes());

            String response = "";

            if (uri.matches("/todo/task/?")) {
                switch (HttpMethod.valueOf(method)) {
                    case POST:
                        response = handlePOST(userData,body);
                        exchange.sendResponseHeaders(CREATED.getCode(), response.length());
                        break;
                    case GET:
                        response = handleGET(userData);
                        exchange.sendResponseHeaders(OK.getCode(), response.length());
                        break;
                }
            }
            else if(uri.matches(uuidPattern)) {

                var urlSplited = uri.split("/");
                var id = urlSplited[3];

                switch (HttpMethod.valueOf(method)) {
                    case GET:
                        response=handleGET_ID(userData,id);
                        exchange.sendResponseHeaders(OK.getCode(), response.length());
                        break;
                    case PUT:
                        response=handlePUT_ID(userData,body,id);
                        exchange.sendResponseHeaders(OK.getCode(), response.length());
                        break;
                    case DELETE:
                        response = handleDELETE_ID(userData,id);
                        exchange.sendResponseHeaders(OK.getCode(), response.length());
                        break;
                }
            }

            if(!response.isEmpty()) {
                var responseBody = exchange.getResponseBody();
                responseBody.write(response.getBytes());
                responseBody.close();
                exchange.close();
            }
            else {
                LOGGER.warning("URI doesnt match!");
                exchange.sendResponseHeaders(BAD_REQUEST.getCode(), 0);
            }

        } catch (BadRequestException | IOException e ) {
            LOGGER.warning(e.getMessage());
            exchange.sendResponseHeaders(BAD_REQUEST.getCode(), 0);
        } catch (UnauthorizedException  e) {
            LOGGER.warning(e.getMessage());
            exchange.sendResponseHeaders(UNAUTHORIZED.getCode(), 0);
        }catch (ForbiddenException  e) {
            LOGGER.warning(e.getMessage());
            exchange.sendResponseHeaders(FORBIDDEN.getCode(), 0);
        }catch (NotFoundException  e) {
            LOGGER.warning(e.getMessage());
            exchange.sendResponseHeaders(NOT_FOUND.getCode(), 0);
        } catch (Exception e) {
            LOGGER.warning("UNCATCHED PROBLEM");
            LOGGER.warning(e.getMessage());
        } finally {
            exchange.close();
        }
    }

    private String handlePOST(List<String> userData,String body)
            throws IOException, BadRequestException, UnauthorizedException {

        var task = gson.fromJson(body, TaskEntity.class);

        service.validateTask(task);
        service.validateUser(userData);
        
        UUID taskId = service.saveTask(task, userData.get(0));

        var responseJson = new JsonObject();
        responseJson.addProperty("id", String.valueOf(taskId));

        LOGGER.info("SERVER: Task \"" + task.getId() + "\" has been added to task-repository.");
        return responseJson.toString();
    }

    private String handleGET(List<String> userData) throws IOException, BadRequestException, UnauthorizedException {

        service.validateUser(userData);

        List<TaskEntity> listOfTasks = service.getTasks(userData.get(0));

        LOGGER.info("SERVER: List of tasks \"" + userData.get(0) + "\" has been sent.");
        return SendResponseListOfTasks(listOfTasks);
    }

    private String handleGET_ID(List<String> userData,String id)
            throws BadRequestException, UnauthorizedException, NotFoundException, ForbiddenException, IOException {

        service.validateUser(userData);

        TaskEntity task = service.getTask(id);

        service.check_taskExists_belongsToUser(task,userData.get(0));

        LOGGER.info("SERVER: Task \"" + id + "\" has been sent.");
        return SendResponseTask(task);
    }

    private String handlePUT_ID(List<String> userData,String body,String id)
            throws BadRequestException, UnauthorizedException, NotFoundException, ForbiddenException, IOException {

        var task = gson.fromJson(body, TaskEntity.class);
        var uuid = UUID.fromString(id);

        service.validateTask(task);
        service.validateUser(userData);

        TaskEntity taskToUpdate = service.getTask(id);

        service.check_taskExists_belongsToUser(taskToUpdate,userData.get(0));

        var updatedTask=service.updateTask(task,userData,uuid);

        LOGGER.info("SERVER: Task \"" + id + "\" has been updated.");
        return SendResponseTask(updatedTask);
    }
    
    private String handleDELETE_ID(List<String> userData,String id) throws BadRequestException, IOException, NotFoundException, ForbiddenException, UnauthorizedException {

        var uuid =UUID.fromString(id);

        service.validateUser(userData);

        TaskEntity taskToDelete = service.getTask(id);

        service.check_taskExists_belongsToUser(taskToDelete,userData.get(0));

        service.deleteTask(uuid);

        LOGGER.info("SERVER: \nTask \"" + id + "\" has been deleted.");
        return "Task \" "+ id + "\" has been deleted.";
    }

    private List<String> validateAuthHeader(String auth) throws BadRequestException {

        if (auth == null)
            throw new BadRequestException("header==null");

        String[] userData = auth.split(":");
        if (userData.length != 2)
            throw new BadRequestException("Invalid token format");

        for (String s : userData) {
            if (s == null || !s.matches(base64Pattern))
                throw new BadRequestException("Invalid token format");
        }

        var decodedUsername = new String(decoder.decode(userData[0]));
        var decodedPassword = new String(decoder.decode(userData[1]));

        List<String> userDecodedData = new ArrayList<>();
        userDecodedData.add(decodedUsername);
        userDecodedData.add(decodedPassword);
        return userDecodedData;
    }

    private String SendResponseTask(TaskEntity task){
        var responseJson = new JsonObject();
        responseJson.addProperty("id", String.valueOf(task.getId()));
        responseJson.addProperty("description", task.getDescription());
        if(task.getDue()!=null)
            responseJson.addProperty("due", task.getDue());

        return responseJson.toString();
    }

    private String SendResponseListOfTasks(List<TaskEntity> listOfTasks) {

        var responseJsonArray = new JsonArray();
        for (TaskEntity task : listOfTasks) {
            var responseJson = new JsonObject();
            responseJson.addProperty("id", String.valueOf(task.getId()));
            responseJson.addProperty("description", task.getDescription());
            if (task.getDue() != null)
                responseJson.addProperty("due", task.getDue());
            responseJsonArray.add(responseJson);
        }
        return responseJsonArray.toString();
    }
}
