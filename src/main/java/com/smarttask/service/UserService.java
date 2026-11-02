package com.smarttask.service;

import com.smarttask.model.User;
import com.smarttask.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class UserService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor Injection
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Register a new user
    public User registerUser(User user) {

        // Validate required fields
        if (user.getFullName() == null ||
            user.getFullName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                "Name cannot be empty"
            );
        }

        if (user.getEmail() == null ||
            user.getEmail().trim().isEmpty()) {

            throw new IllegalArgumentException(
                "Email cannot be empty"
            );
        }

        if (user.getPasswordHash() == null ||
            user.getPasswordHash().length() < 8) {

            throw new IllegalArgumentException(
                "Password must contain at least 8 characters"
            );
        }

        // Check duplicate email
        if (userRepository.getUserByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException(
                "Email is already registered"
            );
        }

        // Every public registration creates a normal USER
        user.setRole("USER");
        user.setAdminId(null);
        user.setActive(true);

        // Hash password using BCrypt
        String hashedPassword =
            passwordEncoder.encode(user.getPasswordHash());

        user.setPasswordHash(hashedPassword);

        // Save user
        return userRepository.saveUser(user);
    }

    // 2. Retrieve user by ID
    public User getUserById(Long userId) {

        User user = userRepository.getUserById(userId);

        if (user == null) {
            throw new IllegalArgumentException(
                "User not found"
            );
        }

        return user;
    }

    // 3. Retrieve user by email
    public User getUserByEmail(String email) {

        User user = userRepository.getUserByEmail(email);

        if (user == null) {
            throw new IllegalArgumentException(
                "User not found"
            );
        }

        return user;
    }

    // 4. Retrieve all users
    public List<User> getAllUsers() {

        return userRepository.getAllUsers();
    }

    // 5. Update user profile details
    public User updateUser(User user) {

        User existingUser = getUserById(user.getUserId());

        // Check whether the new email belongs to another user
        User emailUser =
            userRepository.getUserByEmail(user.getEmail());

        if (emailUser != null &&
            !emailUser.getUserId().equals(user.getUserId())) {

            throw new IllegalArgumentException(
                "Email is already registered"
            );
        }

        // Preserve security-sensitive fields
        user.setPasswordHash(existingUser.getPasswordHash());
        user.setRole(existingUser.getRole());
        user.setAdminId(existingUser.getAdminId());
        user.setActive(existingUser.isActive());

        boolean updated = userRepository.updateUser(user);

        if (!updated) {
            throw new IllegalArgumentException(
                "Unable to update user"
            );
        }

        return user;
    }

    // 6. Deactivate user
    public boolean deactivateUser(Long userId) {

        User user = getUserById(userId);

        user.setActive(false);

        return userRepository.updateUser(user);
    }

    // 7. Activate user
    public boolean activateUser(Long userId) {

        User user = getUserById(userId);

        user.setActive(true);

        return userRepository.updateUser(user);
    }

    // 8. Verify password during login
    public boolean verifyPassword(
            String rawPassword,
            String hashedPassword) {

        return passwordEncoder.matches(
            rawPassword,
            hashedPassword
        );
    }
}
