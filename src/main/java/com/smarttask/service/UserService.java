package com.smarttask.service;

import com.smarttask.model.User;
import com.smarttask.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class UserService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor Injection
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // 1. Register a new user
    public User registerUser(User user) {

        // Check whether email already exists
        if (userRepository.getUserByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        // Validate user role
        if (!isValidRole(user.getRole())) {
            throw new IllegalArgumentException(
                    "Invalid user role"
            );
        }

        // Hash password before storing it
        String hashedPassword =
                passwordEncoder.encode(user.getPasswordHash());

        user.setPasswordHash(hashedPassword);

        // Save user in database
        return userRepository.saveUser(user);
    }

    // 2. Validate user role
    private boolean isValidRole(String role) {

        return role != null &&
                (role.equals("SUPER_ADMIN") ||
                 role.equals("ADMIN") ||
                 role.equals("USER"));
    }

    // 3. Retrieve user by ID
    public User getUserById(Long userId) {

        User user = userRepository.getUserById(userId);

        if (user == null) {
            throw new IllegalArgumentException(
                    "User not found"
            );
        }

        return user;
    }

    // 4. Retrieve user by email
    public User getUserByEmail(String email) {

        User user = userRepository.getUserByEmail(email);

        if (user == null) {
            throw new IllegalArgumentException(
                    "User not found"
            );
        }

        return user;
    }

    // 5. Retrieve all users
    public List<User> getAllUsers() {

        return userRepository.getAllUsers();
    }

    // 6. Update user details
    public User updateUser(User user) {

        getUserById(user.getUserId());

        if (!isValidRole(user.getRole())) {
            throw new IllegalArgumentException(
                    "Invalid user role"
            );
        }

        boolean updated = userRepository.updateUser(user);

        if (!updated) {
            throw new IllegalArgumentException(
                    "Unable to update user"
            );
        }

        return user;
    }

    // 7. Deactivate user account
    public boolean deactivateUser(Long userId) {

        User user = getUserById(userId);

        user.setActive(false);

        return userRepository.updateUser(user);
    }

    // 8. Activate user account
    public boolean activateUser(Long userId) {

        User user = getUserById(userId);

        user.setActive(true);

        return userRepository.updateUser(user);
    }

    // 9. Verify password
    public boolean verifyPassword(
            String rawPassword, String hashedPassword) {

        return passwordEncoder.matches(
                rawPassword, hashedPassword
        );
    }
}