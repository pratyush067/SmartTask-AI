package com.smarttask.controller;

import com.smarttask.model.User;
import com.smarttask.service.UserService;
import com.smarttask.dto.CreateAdminRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

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
    public ResponseEntity<List<User>> getAllUsers(
            Authentication authentication) {

        User actor = userService.getUserByEmail(
            authentication.getName()
        );

        List<User> users = userService.getAllUsers(actor);

        return ResponseEntity.ok(removePasswords(users));
    }

    // 2. Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @PathVariable Long id,
            Authentication authentication) {

        User actor = userService.getUserByEmail(
            authentication.getName()
        );

        User user = userService.getUserById(id, actor);

        return ResponseEntity.ok(removePassword(user));
    }

    // 3. Get user by email
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(
            @PathVariable String email,
            Authentication authentication) {

        User actor = userService.getUserByEmail(
            authentication.getName()
        );

        User user = userService.getUserByEmail(email, actor);

        return ResponseEntity.ok(removePassword(user));
    }

    // 4. Update user profile
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User user,
            Authentication authentication) {

        User actor = userService.getUserByEmail(
            authentication.getName()
        );

        user.setUserId(id);

        User updatedUser = userService.updateUser(user, actor);

        return ResponseEntity.ok(removePassword(updatedUser));
    }

    // 5. Deactivate user
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(
            @PathVariable Long id,
            Authentication authentication) {

        User actor = userService.getUserByEmail(
            authentication.getName()
        );

        userService.deactivateUser(id, actor);

        return ResponseEntity.ok("User deactivated successfully");
    }

    // 6. Activate user
    @PutMapping("/{id}/activate")
    public ResponseEntity<String> activateUser(
            @PathVariable Long id,
            Authentication authentication) {

        User actor = userService.getUserByEmail(
            authentication.getName()
        );

        userService.activateUser(id, actor);

        return ResponseEntity.ok("User activated successfully");
    }

    // 7. Create Admin account
    @PostMapping("/admin")
    public ResponseEntity<?> createAdmin(
            @RequestBody CreateAdminRequest request,
            Authentication authentication) {

        try {
            User user = new User();

            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPasswordHash(request.getPassword());

            User actor = userService.getUserByEmail(
                authentication.getName()
            );

            User createdAdmin = userService.createAdmin(user, actor);

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
            @RequestParam Long adminId,
            Authentication authentication) {

        try {

            User actor = userService.getUserByEmail(
                authentication.getName()
            );

            userService.assignUserToAdmin(
                userId,
                adminId,
                actor
            );

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
            @PathVariable Long adminId,
            Authentication authentication) {

        try {

            User actor = userService.getUserByEmail(
                authentication.getName()
            );

            List<User> users =
                userService.getUsersByAdminId(adminId, actor);

            return ResponseEntity.ok(removePasswords(users));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
    
    // 10. Get all unassigned Users
    @GetMapping("/unassigned")
    public ResponseEntity<List<User>> getUnassignedUsers(
            Authentication authentication) {

        User actor = userService.getUserByEmail(
            authentication.getName()
        );

        List<User> users = userService.getUnassignedUsers(actor);

        return ResponseEntity.ok(removePasswords(users));
    }

    // Change user role
    @PutMapping("/{id}/role")
    public ResponseEntity<?> changeUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            Authentication authentication) {

        try {

            User actor = userService.getUserByEmail(
                authentication.getName()
            );

            userService.changeUserRole(id, role, actor);

            return ResponseEntity.ok(
                "User role updated successfully."
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}