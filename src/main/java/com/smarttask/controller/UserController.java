package com.smarttask.controller;

import com.smarttask.model.User;
import com.smarttask.service.UserService;
import com.smarttask.dto.CreateAdminRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController
{
    private final UserService userService;

    // Constructor Injection
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Remove password hash before returning user details
    private User removePassword(User user) {
        if (user != null) {
            user.setPasswordHash(null);
        }
        return user;
    }

    // Remove password hashes from a list of users
    private List<User> removePasswords(List<User> users) {
        for (User user : users) {
            removePassword(user);
        }
        return users;
    }

    // 1. Get all users
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {

        List<User> users = userService.getAllUsers();

        return ResponseEntity.ok(removePasswords(users));
    }

    // 2. Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @PathVariable Long id) {

        User user = userService.getUserById(id);

        return ResponseEntity.ok(removePassword(user));
    }

    // 3. Get user by email
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(
            @PathVariable String email) {

        User user = userService.getUserByEmail(email);

        return ResponseEntity.ok(removePassword(user));
    }

    // 4. Update user profile
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User user) {

        user.setUserId(id);

        User updatedUser = userService.updateUser(user);

        return ResponseEntity.ok(removePassword(updatedUser));
    }

    // 5. Deactivate user
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(
            @PathVariable Long id) {

        userService.deactivateUser(id);

        return ResponseEntity.ok(
            "User deactivated successfully"
        );
    }

    // 6. Activate user
    @PutMapping("/{id}/activate")
    public ResponseEntity<String> activateUser(
            @PathVariable Long id) {

        userService.activateUser(id);

        return ResponseEntity.ok(
            "User activated successfully"
        );
    }

    // 7. Create Admin account
    @PostMapping("/admin")
    public ResponseEntity<?> createAdmin(
            @RequestBody CreateAdminRequest request) {

        try {
            User user = new User();

            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPasswordHash(request.getPassword());

            User createdAdmin = userService.createAdmin(user);

            return ResponseEntity.status(201)
                    .body(removePassword(createdAdmin));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // 8. Assign a User to an Admin
    @PostMapping("/assign")
    public ResponseEntity<?> assignUserToAdmin(
            @RequestParam Long userId,
            @RequestParam Long adminId) {

        try {
            userService.assignUserToAdmin(userId, adminId);

            return ResponseEntity.ok(
                "User assigned to Admin successfully"
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // 9. Get all Users assigned to an Admin
    @GetMapping("/admin/{adminId}")
    public ResponseEntity<?> getUsersByAdminId(
            @PathVariable Long adminId) {

        try {
            List<User> users =
                userService.getUsersByAdminId(adminId);

            return ResponseEntity.ok(removePasswords(users));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // 10. Get all unassigned Users
    @GetMapping("/unassigned")
    public ResponseEntity<List<User>> getUnassignedUsers() {

        List<User> users = userService.getUnassignedUsers();

        return ResponseEntity.ok(removePasswords(users));
    }
}