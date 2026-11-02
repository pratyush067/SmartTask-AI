package com.smarttask.controller;

import com.smarttask.model.User;
import com.smarttask.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController
{
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Register a new user
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {

        User savedUser = userService.registerUser(user);

        // Never expose the password hash in API responses
        savedUser.setPasswordHash(null);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // Get all users
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {

        List<User> users = userService.getAllUsers();

        // Hide password hashes
        for (User user : users) {
            user.setPasswordHash(null);
        }

        return ResponseEntity.ok(users);
    }

    // Get a user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {

        User user = userService.getUserById(id);

        user.setPasswordHash(null);

        return ResponseEntity.ok(user);
    }

    // Get a user by email
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(
            @PathVariable String email) {

        User user = userService.getUserByEmail(email);

        user.setPasswordHash(null);

        return ResponseEntity.ok(user);
    }

    // Update user details
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User user) {

        user.setUserId(id);

        User updatedUser = userService.updateUser(user);

        updatedUser.setPasswordHash(null);

        return ResponseEntity.ok(updatedUser);
    }

    // Deactivate a user
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable Long id) {

        userService.deactivateUser(id);

        return ResponseEntity.ok("User deactivated successfully.");
    }

    // Activate a user
    @PutMapping("/{id}/activate")
    public ResponseEntity<String> activateUser(@PathVariable Long id) {

        userService.activateUser(id);

        return ResponseEntity.ok("User activated successfully.");
    }
}