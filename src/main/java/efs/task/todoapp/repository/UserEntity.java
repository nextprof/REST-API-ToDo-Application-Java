package efs.task.todoapp.repository;

public class UserEntity {

    private final String username;
    private final String password;


    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
