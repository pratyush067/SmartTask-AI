package com.smarttask.repository;

import com.smarttask.model.User;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class UserRepository 
{
    private final JdbcTemplate jdbcTemplate;

    // Constructor Injection
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Maps database records to User objects
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {

        User user = new User();

        user.setUserId(rs.getLong("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));

        long adminId = rs.getLong("admin_id");

        if (!rs.wasNull()) {
            user.setAdminId(adminId);
        }

        user.setActive(rs.getBoolean("is_active"));

        if (rs.getTimestamp("created_at") != null) {
            user.setCreatedAt(
                rs.getTimestamp("created_at").toLocalDateTime()
            );
        }

        return user;
    };

    // 1. Insert a new user
    public User saveUser(User user) {

        String sql = "INSERT INTO users " +
                "(full_name, email, password_hash, role, admin_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.setObject(5, user.getAdminId());

            return ps;

        }, keyHolder);

        if (keyHolder.getKey() != null) {
            user.setUserId(keyHolder.getKey().longValue());
        }

        return user;
    }

    // 2. Find user by ID
    public User getUserById(Long userId) {

        String sql = "SELECT * FROM users WHERE user_id = ?";

        List<User> users = jdbcTemplate.query(
            sql, userRowMapper, userId
        );

        return users.isEmpty() ? null : users.get(0);
    }

    // 3. Find user by email
    public User getUserByEmail(String email) {

        String sql = "SELECT * FROM users WHERE email = ?";

        List<User> users = jdbcTemplate.query(
            sql, userRowMapper, email
        );

        return users.isEmpty() ? null : users.get(0);
    }

    // 4. Retrieve all users
    public List<User> getAllUsers() {

        String sql = "SELECT * FROM users ORDER BY user_id";

        return jdbcTemplate.query(sql, userRowMapper);
    }

    // 5. Update user details
    public boolean updateUser(User user) {

        String sql = "UPDATE users SET " +
                "full_name = ?, email = ?, role = ?, " +
                "admin_id = ?, is_active = ? " +
                "WHERE user_id = ?";

        int rows = jdbcTemplate.update(
            sql,
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.getAdminId(),
            user.isActive(),
            user.getUserId()
        );

        return rows > 0;
    }

    // Update password hash
    public boolean updatePasswordHash(
            Long userId,
            String passwordHash) {

        String sql = "UPDATE users SET password_hash = ? " +
                     "WHERE user_id = ?";

        int rows = jdbcTemplate.update(
            sql,
            passwordHash,
            userId
        );

        return rows > 0;
    }

    // 6. Delete user
    public boolean deleteUser(Long userId) {

        String sql = "DELETE FROM users WHERE user_id = ?";

        int rows = jdbcTemplate.update(sql, userId);

        return rows > 0;
    }

    // 7. Assign a registered user to an Admin
    public boolean assignUserToAdmin(Long userId, Long adminId) {

        String sql = "UPDATE users SET admin_id = ? " +
                    "WHERE user_id = ? AND role = 'USER'";

        int rows = jdbcTemplate.update(
            sql,
            adminId,
            userId
        );

        return rows > 0;
    }

    // 8. Retrieve all users assigned to a particular Admin
    public List<User> getUsersByAdminId(Long adminId) {

        String sql = "SELECT * FROM users " +
                    "WHERE admin_id = ? AND role = 'USER' " +
                    "ORDER BY user_id";

        return jdbcTemplate.query(
            sql,
            userRowMapper,
            adminId
        );
    }

    // 9. Retrieve users who have not been assigned to an Admin
    public List<User> getUnassignedUsers() {

        String sql = "SELECT * FROM users " +
                    "WHERE admin_id IS NULL AND role = 'USER' " +
                    "ORDER BY user_id";

        return jdbcTemplate.query(
            sql,
            userRowMapper
        );
    }
}
