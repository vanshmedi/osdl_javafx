package com.hotel.service;

import com.hotel.model.Role;
import com.hotel.model.User;
import com.hotel.util.SerializationUtil;

import java.util.ArrayList;
import java.util.List;

public class AuthService {
    private static final String USERS_FILE = "users_data.dat";
    private List<User> users;

    @SuppressWarnings("unchecked")
    public AuthService() {
        users = new ArrayList<>();
        Object data = SerializationUtil.deserialize(USERS_FILE);
        if (data instanceof List) {
            users = (List<User>) data;
            System.out.println("Loaded " + users.size() + " users.");
        } else {
            // First run setup
            users.add(new User("admin", "admin", Role.ADMIN));
            users.add(new User("frontdesk", "password", Role.RECEPTIONIST));
            saveUsers();
            System.out.println("Default users created.");
        }
    }

    private void saveUsers() {
        SerializationUtil.serialize(users, USERS_FILE);
    }

    public void addUser(User user) {
        for (User u : users) {
             if (u.getUsername().equals(user.getUsername())) {
                 throw new IllegalArgumentException("Username already exists.");
             }
        }
        users.add(user);
        saveUsers();
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public User login(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }
}
