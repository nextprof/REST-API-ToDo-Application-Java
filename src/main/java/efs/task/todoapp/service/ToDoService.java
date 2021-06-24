package efs.task.todoapp.service;

import efs.task.todoapp.repository.TaskEntity;
import efs.task.todoapp.repository.TaskRepository;
import efs.task.todoapp.repository.UserEntity;
import efs.task.todoapp.repository.UserRepository;
import efs.task.todoapp.service.Exceptions.*;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class ToDoService {
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    public ToDoService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    public void saveUser(UserEntity user) throws BadRequestException, ConflictException {

        if(!isUserValid(user))
            throw new BadRequestException("User data are not valid");

        String idUser = user.getUsername();

        if (userRepository.doesUserExists(idUser)) {
            throw new ConflictException("User \"" + idUser + "\" already exists, cannot be added to repository.");
        } else {
           userRepository.save(user);
        }
    }

    public UUID saveTask(TaskEntity task, String username) {

        UUID idTask = UUID.randomUUID();
        task.setId(idTask);
        task.setOwner(username);

        taskRepository.save(task);

        return idTask;
    }

    public TaskEntity updateTask(TaskEntity task,List<String> userData,UUID uuid){

        var updatedTask=taskRepository.update(uuid,task);
        task.setId(uuid);
        task.setOwner(userData.get(0));
        return updatedTask;
    }

    public void deleteTask(UUID uuid){
        taskRepository.delete(uuid);
    }

    public List<TaskEntity> getTasks(String username) {
        return taskRepository.query(alwaysTrue(username));
    }

    public TaskEntity getTask(String uuid){
        return taskRepository.query(UUID.fromString(uuid));
    }

    public static Predicate<TaskEntity> alwaysTrue(String username) {
        return p -> p.getOwner().equals(username);
    }

    public boolean isUserValid(UserEntity user) {
        return user != null && user.getUsername() != null && user.getPassword() != null
                && !user.getPassword().isEmpty() && !user.getUsername().isEmpty();
    }

    public void validateUser(List<String> userData) throws UnauthorizedException {

        UserEntity user = userRepository.query(userData.get(0));

        if(user == null || !user.getPassword().equals(userData.get(1)))
            throw new UnauthorizedException("User not found/ user's password doesnt match!");
    }

    public void validateTask(TaskEntity task) throws BadRequestException {
        if(task != null && task.getDescription() != null && !task.getDescription().equals(""))
        {
            if(task.getDue()!=null) {
                try {
                    java.time.format.DateTimeFormatter.ISO_DATE.parse(task.getDue());
                } catch (Exception e) {
                    throw new BadRequestException("Invalid data format");
                }
            }
        }
        else{ throw new BadRequestException("Invalid body task");}
    }

    public void check_taskExists_belongsToUser(TaskEntity task,String username) throws ForbiddenException, NotFoundException {
        if (task==null)
            throw new NotFoundException("Task doesn't exists.");

        if(!task.getOwner().equals(username))
            throw new ForbiddenException("This task in connected to another user.");
    }
}
