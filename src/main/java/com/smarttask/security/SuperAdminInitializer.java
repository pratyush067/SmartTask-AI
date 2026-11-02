package com.smarttask.security;

import com.smarttask.model.User;
import com.smarttask.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SuperAdminInitializer
{
    @Value("${app.super-admin.name:Super Admin}")
    private String adminName;

    @Value("${app.super-admin.email:}")
    private String adminEmail;

    @Value("${app.super-admin.password:}")
    private String adminPassword;

    @Bean
    public CommandLineRunner createSuperAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // Check whether Super Admin credentials are configured
            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                System.out.println(
                    "Super Admin credentials not configured. Skipping initialization."
                );
                return;
            }

            // Check whether this email already exists
            User existingUser =
                userRepository.getUserByEmail(adminEmail);

            if (existingUser != null) {
                System.out.println(
                    "Super Admin account already exists."
                );
                return;
            }

            // Create the initial Super Admin
            User admin = new User();

            admin.setFullName(adminName);
            admin.setEmail(adminEmail);
            admin.setPasswordHash(
                passwordEncoder.encode(adminPassword)
            );
            admin.setRole("SUPER_ADMIN");
            admin.setAdminId(null);
            admin.setActive(true);

            userRepository.saveUser(admin);

            System.out.println(
                "Initial Super Admin created successfully."
            );
        };
    }
}