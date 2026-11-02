package com.smarttask.security;

import com.smarttask.model.User;
import com.smarttask.service.UserService;

import org.springframework.stereotype.Service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
public class CustomUserDetailsService implements UserDetailsService
{
    private final UserService userService;

    // Constructor Injection
    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    // Load user using email address
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user;

        try {
            user = userService.getUserByEmail(email);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException(
                "User not found with email: " + email
            );
        }

        // Convert our User model into Spring Security UserDetails
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .disabled(!user.isActive())
                .build();
    }
}
