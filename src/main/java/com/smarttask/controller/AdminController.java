package com.smarttask.controller;

import com.smarttask.model.User;
import com.smarttask.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // Get Users assigned to the logged-in Admin
    @GetMapping("/team")
    public ResponseEntity<?> getMyTeam(Authentication authentication) {

        // Get the logged-in Admin's email
        String email = authentication.getName();

        // Find the Admin using their email
        User admin = userService.getUserByEmail(email);

        // Retrieve only Users assigned to this Admin
        List<User> users =
                userService.getUsersByAdminId(admin.getUserId(), admin);

        // Prepare safe response without password hashes
        List<Map<String, Object>> team = new ArrayList<>();

        for (User user : users) {

            Map<String, Object> userDetails = new LinkedHashMap<>();

            userDetails.put("userId", user.getUserId());
            userDetails.put("fullName", user.getFullName());
            userDetails.put("email", user.getEmail());
            userDetails.put("role", user.getRole());
            userDetails.put("active", user.isActive());
            userDetails.put("createdAt", user.getCreatedAt());

            team.add(userDetails);
        }

        return ResponseEntity.ok(team);
    }
}
