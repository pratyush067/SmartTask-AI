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

        if (userRepository.getUserByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException(
                "Email is already registered"
            );
        }

        // Public registration always creates a normal USER
        user.setRole("USER");
        user.setAdminId(null);
        user.setActive(true);

        // Hash password using BCrypt
        String hashedPassword =
            passwordEncoder.encode(user.getPasswordHash());

        user.setPasswordHash(hashedPassword);

        return userRepository.saveUser(user);
    }

    // 2. Create a new Admin
    public User createAdmin(User user) {

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

        if (userRepository.getUserByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException(
                "Email is already registered"
            );
        }

        user.setRole("ADMIN");
        user.setAdminId(null);
        user.setActive(true);

        // Hash password before storing
        String hashedPassword =
            passwordEncoder.encode(user.getPasswordHash());

        user.setPasswordHash(hashedPassword);

        return userRepository.saveUser(user);
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

    // 6. Update user profile details
    public User updateUser(User user) {

        User existingUser = getUserById(user.getUserId());

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

    // 7. Deactivate user
    public boolean deactivateUser(Long userId) {

        User user = getUserById(userId);

        user.setActive(false);

        return userRepository.updateUser(user);
    }

    // 8. Activate user
    public boolean activateUser(Long userId) {

        User user = getUserById(userId);

        user.setActive(true);

        return userRepository.updateUser(user);
    }

    // 9. Verify password during login
    public boolean verifyPassword(
            String rawPassword,
            String hashedPassword) {

        return passwordEncoder.matches(
            rawPassword,
            hashedPassword
        );
    }

    // 10. Assign a registered User to an Admin
    public boolean assignUserToAdmin(Long userId, Long adminId) {

        // Check whether User exists
        User user = userRepository.getUserById(userId);

        if (user == null) {
            throw new IllegalArgumentException(
                "User not found"
            );
        }

        // Check whether Admin exists
        User admin = userRepository.getUserById(adminId);

        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            throw new IllegalArgumentException(
                "Invalid Admin ID"
            );
        }

        // Only normal Users can be assigned
        if (!"USER".equals(user.getRole())) {
            throw new IllegalArgumentException(
                "Only regular Users can be assigned"
            );
        }

        // Prevent accidental reassignment
        if (user.getAdminId() != null) {
            throw new IllegalArgumentException(
                "User is already assigned to an Admin"
            );
        }

        // Both accounts must be active
        if (!user.isActive() || !admin.isActive()) {
            throw new IllegalArgumentException(
                "Both User and Admin must be active"
            );
        }

        return userRepository.assignUserToAdmin(
            userId,
            adminId
        );
    }


    // 11. Get all Users assigned to a particular Admin
    public List<User> getUsersByAdminId(Long adminId) {

        User admin = userRepository.getUserById(adminId);

        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            throw new IllegalArgumentException(
                "Invalid Admin ID"
            );
        }

        return userRepository.getUsersByAdminId(adminId);
    }


    // 12. Get all Users not assigned to any Admin
    public List<User> getUnassignedUsers() {

        return userRepository.getUnassignedUsers();
    }

}
