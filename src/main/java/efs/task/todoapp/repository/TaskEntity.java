package efs.task.todoapp.repository;

import java.util.UUID;

public class TaskEntity {

    private UUID id;
    private String description;
    private String due;

    private String owner;

    public TaskEntity(String description) {
        this.description = description;
    }

    public TaskEntity(String description,String due) {
        this.description = description;
        this.due= due;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDue() {
        return due;
    }

    public void setDue(String due) {
        this.due = due;
    }
}
