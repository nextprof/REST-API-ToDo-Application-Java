package efs.task.todoapp.web;

public enum HttpCode {
    OK(200),
    CREATED(201),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409);

    private final int code;

    HttpCode(int statusCode) { this.code = statusCode; }

    public int getCode() {
        return code;
    }
}
