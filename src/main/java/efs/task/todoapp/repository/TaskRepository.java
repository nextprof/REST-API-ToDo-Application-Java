package efs.task.todoapp.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TaskRepository implements Repository<UUID, TaskEntity> {

    private final Map<UUID,TaskEntity> tasks;

    public TaskRepository() {
        this.tasks= new ConcurrentHashMap<>();
    }

    @Override
    public UUID save(TaskEntity taskEntity) {
        TaskEntity result = tasks.putIfAbsent(taskEntity.getId(), taskEntity);
        if (result == null) {
            return taskEntity.getId();
        } else {
            return null;
        }
    }

    @Override
    public TaskEntity query(UUID uuid) {
        return tasks.getOrDefault(uuid,null);
    }

    @Override
    public List<TaskEntity> query(Predicate<TaskEntity> condition) {
        return tasks.values().stream().filter(condition).collect(Collectors.toList());
    }

    @Override
    public TaskEntity update(UUID uuid, TaskEntity taskEntity) {
        if (tasks.containsKey(uuid)) {
            tasks.replace(uuid, taskEntity);
            return taskEntity;
        } else {
            return null;
        }
    }

    @Override
    public boolean delete(UUID uuid) {
        return tasks.remove(uuid) != null;
    }
}
