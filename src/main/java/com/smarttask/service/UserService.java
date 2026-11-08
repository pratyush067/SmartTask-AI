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
    public User createAdmin(User user, User actor) {
        requireSuperAdmin(actor);
        
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
    public List<User> getAllUsers(User actor) {

        requireSuperAdmin(actor);

        return userRepository.getAllUsers();
    }

    // 6. Update user profile details
    public User updateUser(User user, User actor) {
        
        validateActor(actor);

        // Only Super Admin can update another user's profile
        if (!"SUPER_ADMIN".equals(actor.getRole()) &&
            !actor.getUserId().equals(user.getUserId())) {

            throw new IllegalArgumentException(
                "You are not authorized to update this profile."
            );
        }

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
    public boolean deactivateUser(Long userId, User actor) {

        requireAdminOrSuperAdmin(actor);

        User user = getUserById(userId);

        if (actor.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                "You cannot deactivate your own account."
            );
        }

        if ("ADMIN".equals(actor.getRole()) &&
            !isTeamMember(actor, user)) {

            throw new IllegalArgumentException(
                "You can only deactivate your own team members."
            );
        }

        user.setActive(false);

        return userRepository.updateUser(user);
    }

    // 8. Activate user
    public boolean activateUser(Long userId, User actor) {

        requireAdminOrSuperAdmin(actor);

        User user = getUserById(userId);

        if (actor.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                "You cannot modify your own account through this operation."
            );
        }

        if ("ADMIN".equals(actor.getRole()) &&
            !isTeamMember(actor, user)) {

            throw new IllegalArgumentException(
                "You can only activate your own team members."
            );
        }

        return userRepository.updateUser(
            setUserActive(user, true)
        );
    }

    
    // Change password of the currently logged-in user
    public void changePassword(
            String email,
            String currentPassword,
            String newPassword,
            String confirmNewPassword) {

        User user = getUserByEmail(email);

        validateActor(user);

        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new IllegalArgumentException(
                "Current password is required."
            );
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException(
                "New password must contain at least 8 characters."
            );
        }

        if (!newPassword.equals(confirmNewPassword)) {
            throw new IllegalArgumentException(
                "New password and confirmation do not match."
            );
        }

        if (!passwordEncoder.matches(
                currentPassword, user.getPasswordHash())) {

            throw new IllegalArgumentException(
                "Current password is incorrect."
            );
        }

        if (passwordEncoder.matches(
                newPassword, user.getPasswordHash())) {

            throw new IllegalArgumentException(
                "New password must be different from the current password."
            );
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);

        boolean updated = userRepository.updatePasswordHash(
            user.getUserId(),
            newPasswordHash
        );

        if (!updated) {
            throw new IllegalArgumentException(
                "Unable to update password. Please try again."
            );
        }
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
    public boolean assignUserToAdmin(Long userId, Long adminId, User actor) {

        requireSuperAdmin(actor);

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


    // 11. Get team members of an Admin
    public List<User> getUsersByAdminId(
            Long adminId,
            User actor) {

        requireAdminOrSuperAdmin(actor);

        User admin = userRepository.getUserById(adminId);

        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            throw new IllegalArgumentException(
                "Invalid Admin ID"
            );
        }

        // Admins can view only their own team
        if ("ADMIN".equals(actor.getRole()) &&
            !actor.getUserId().equals(adminId)) {

            throw new IllegalArgumentException(
                "You can only view your own team."
            );
        }

        return userRepository.getUsersByAdminId(adminId);
    }


    // 12. Get all Users not assigned to any Admin
    public List<User> getUnassignedUsers(User actor) {

        requireSuperAdmin(actor);

        return userRepository.getUnassignedUsers();
    }

    
    // Validate that the logged-in user exists and is active
    private void validateActor(User actor) {

        if (actor == null || !actor.isActive()) {
            throw new IllegalArgumentException(
                "Invalid or inactive account."
            );
        }
    }

    // Allow only Super Admin
    private void requireSuperAdmin(User actor) {

        validateActor(actor);

        if (!"SUPER_ADMIN".equals(actor.getRole())) {
            throw new IllegalArgumentException(
                "Only Super Admin can perform this operation."
            );
        }
    }

    // Allow Admin and Super Admin
    private void requireAdminOrSuperAdmin(User actor) {

        validateActor(actor);

        if (!"ADMIN".equals(actor.getRole()) &&
            !"SUPER_ADMIN".equals(actor.getRole())) {

            throw new IllegalArgumentException(
                "You are not authorized to perform this operation."
            );
        }
    }

    // Check whether a User belongs to an Admin
    private boolean isTeamMember(User admin, User user) {

            return admin != null &&
                user != null &&
                "ADMIN".equals(admin.getRole()) &&
                "USER".equals(user.getRole()) &&
                admin.getUserId().equals(user.getAdminId());
        }

        private User setUserActive(User user, boolean active) {
        user.setActive(active);
        return user;
    }

    // Retrieve a user only if the actor is authorized
    public User getUserById(Long userId, User actor) {

        validateActor(actor);

        User target = getUserById(userId);

        if ("SUPER_ADMIN".equals(actor.getRole())) {
            return target;
        }

        if (actor.getUserId().equals(userId)) {
            return target;
        }

        if ("ADMIN".equals(actor.getRole()) &&
            isTeamMember(actor, target)) {
            return target;
        }

        throw new IllegalArgumentException(
            "You are not authorized to view this user."
        );
    }

    // Retrieve a user by email only if the actor is authorized
    public User getUserByEmail(String email, User actor) {

        validateActor(actor);

        User target = getUserByEmail(email);

        if ("SUPER_ADMIN".equals(actor.getRole())) {
            return target;
        }

        if (actor.getUserId().equals(target.getUserId())) {
            return target;
        }

        if ("ADMIN".equals(actor.getRole()) &&
            isTeamMember(actor, target)) {
            return target;
        }

        throw new IllegalArgumentException(
            "You are not authorized to view this user."
        );
    }

    // Change the role of an existing user
    public boolean changeUserRole(
            Long userId,
            String newRole,
            User actor) {

        requireSuperAdmin(actor);

        User user = getUserById(userId);

        if ("SUPER_ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException(
                "Super Admin role cannot be changed."
            );
        }

        if (!"USER".equals(newRole) &&
            !"ADMIN".equals(newRole)) {

            throw new IllegalArgumentException(
                "Invalid role. Allowed roles: USER, ADMIN."
            );
        }

        // Prevent demoting an Admin who still has team members
        if ("ADMIN".equals(user.getRole()) &&
            "USER".equals(newRole)) {

            List<User> team =
                userRepository.getUsersByAdminId(userId);

            if (!team.isEmpty()) {
                throw new IllegalArgumentException(
                    "Reassign the Admin's team members before demotion."
                );
            }
        }

        user.setRole(newRole);

        // A normal User promoted to Admin must not retain an Admin ID
        if ("ADMIN".equals(newRole)) {
            user.setAdminId(null);
        }

        return userRepository.updateUser(user);
    }
}
