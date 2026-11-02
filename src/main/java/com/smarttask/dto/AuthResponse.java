package com.smarttask.dto;

public class AuthResponse
{
    private String message;
    private Long userId;
    private String fullName;
    private String email;
    private String role;

    // Default constructor
    public AuthResponse() {
        
    }

    // Parameterized constructor
    public AuthResponse(
            String message,
            Long userId,
            String fullName,
            String email,
            String role) {

        this.message = message;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
