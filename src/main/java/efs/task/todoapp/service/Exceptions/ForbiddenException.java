package efs.task.todoapp.service.Exceptions;

public class ForbiddenException extends Exception{
    public ForbiddenException(String s) {
        super(s);
    }
}