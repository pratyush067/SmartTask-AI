package com.smarttask.controller;

import com.smarttask.dto.RegisterRequest;
import com.smarttask.dto.LoginRequest;
import com.smarttask.dto.AuthResponse;

import com.smarttask.model.User;
import com.smarttask.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController
{
    private final UserService userService;

    // Constructor Injection
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 1. User Registration
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        try {
            // Create User object
            User user = new User();

            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPasswordHash(request.getPassword());

            // UserService hashes password and assigns USER role
            User registeredUser = userService.registerUser(user);

            AuthResponse response = new AuthResponse(
                "Registration successful",
                registeredUser.getUserId(),
                registeredUser.getFullName(),
                registeredUser.getEmail(),
                registeredUser.getRole()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. User Login
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        try {
            // Find user using email
            User user = userService.getUserByEmail(request.getEmail());

            // Check whether account is active
            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Your account is deactivated");
            }

            // Verify entered password against BCrypt hash
            boolean validPassword = userService.verifyPassword(
                request.getPassword(),
                user.getPasswordHash()
            );

            if (!validPassword) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid email or password");
            }

            // Create authentication token with user's role
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    user.getEmail(),
                    null,
                    Collections.singletonList(
                        new SimpleGrantedAuthority(
                            "ROLE_" + user.getRole()
                        )
                    )
                );

            // Create SecurityContext
            SecurityContext context =
                SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            // Create or obtain HTTP session
            HttpSession session = httpRequest.getSession(true);

            // Store authenticated context in session
            session.setAttribute(
                HttpSessionSecurityContextRepository
                    .SPRING_SECURITY_CONTEXT_KEY,
                context
            );

            AuthResponse response = new AuthResponse(
                "Login successful",
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            SecurityContextHolder.clearContext();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }

    // 3. User Logout
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(
            "Logout successful"
        );
    }
}