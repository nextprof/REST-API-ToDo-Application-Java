package efs.task.todoapp.web;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import efs.task.todoapp.repository.UserEntity;
import efs.task.todoapp.service.Exceptions.BadRequestException;
import efs.task.todoapp.service.Exceptions.ConflictException;
import efs.task.todoapp.service.ToDoService;
import static efs.task.todoapp.web.HttpCode.*;

import java.io.IOException;
import java.util.logging.Logger;

public class HttpUserHandler implements HttpHandler {

     private final ToDoService service;
     private static final Logger LOGGER = Logger.getLogger(HttpUserHandler.class.getName());
     private static final Gson gson = new Gson();

     public HttpUserHandler(ToDoService service) { this.service = service; }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            var body = new String(exchange.getRequestBody().readAllBytes());
            var user = gson.fromJson(body, UserEntity.class);

            service.saveUser(user);

            LOGGER.info("User \"" + user.getUsername() + "\" has been added to repository.");
            exchange.sendResponseHeaders(CREATED.getCode(), 0);

        } catch (BadRequestException | IOException e) {
            LOGGER.warning(e.getMessage());
            exchange.sendResponseHeaders(BAD_REQUEST.getCode(), 0);
        } catch (ConflictException e) {
            LOGGER.warning(e.getMessage());
            exchange.sendResponseHeaders(CONFLICT.getCode(), 0);
        } catch (Exception e) {
            LOGGER.warning("UNCACHED PROBLEM/USER");
            LOGGER.warning(e.getMessage());
        } finally {
            exchange.close();
        }
    }
}
