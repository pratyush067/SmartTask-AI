package com.smarttask.repository;

import com.smarttask.model.Notification;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class NotificationRepository 
{
    private final JdbcTemplate jdbcTemplate;

    // Constructor Injection
    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Maps database records to Notification objects
    private final RowMapper<Notification> notificationRowMapper =
            (rs, rowNum) -> {

        Notification notification = new Notification();

        notification.setNotificationId(
                rs.getLong("notification_id")
        );

        notification.setUserId(rs.getLong("user_id"));

        long taskId = rs.getLong("task_id");
        if (!rs.wasNull()) {
            notification.setTaskId(taskId);
        }

        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setNotificationType(
                rs.getString("notification_type")
        );

        notification.setRead(rs.getBoolean("is_read"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(
                    createdAt.toLocalDateTime()
            );
        }

        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) {
            notification.setReadAt(
                    readAt.toLocalDateTime()
            );
        }

        return notification;
    };

    // 1. Create a notification
    public Notification saveNotification(Notification notification) {

        String sql = "INSERT INTO notifications " +
                "(user_id, task_id, title, message, notification_type) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );

            ps.setLong(1, notification.getUserId());
            ps.setObject(2, notification.getTaskId());
            ps.setString(3, notification.getTitle());
            ps.setString(4, notification.getMessage());
            ps.setString(5, notification.getNotificationType());

            return ps;

        }, keyHolder);

        if (keyHolder.getKey() != null) {
            notification.setNotificationId(
                    keyHolder.getKey().longValue()
            );
        }

        return notification;
    }

    // 2. Get all notifications of a user
    public List<Notification> getNotificationsByUserId(Long userId) {

        String sql = "SELECT * FROM notifications " +
                "WHERE user_id = ? ORDER BY created_at DESC";

        return jdbcTemplate.query(
                sql, notificationRowMapper, userId
        );
    }

    // 3. Get unread notifications
    public List<Notification> getUnreadNotifications(Long userId) {

        String sql = "SELECT * FROM notifications " +
                "WHERE user_id = ? AND is_read = FALSE " +
                "ORDER BY created_at DESC";

        return jdbcTemplate.query(
                sql, notificationRowMapper, userId
        );
    }

    // 4. Mark a notification as read
    public boolean markAsRead(Long notificationId) {

        String sql = "UPDATE notifications SET " +
                "is_read = TRUE, read_at = CURRENT_TIMESTAMP " +
                "WHERE notification_id = ? AND is_read = FALSE";

        int rows = jdbcTemplate.update(sql, notificationId);

        return rows > 0;
    }

    // 5. Mark all notifications of a user as read
    public int markAllAsRead(Long userId) {

        String sql = "UPDATE notifications SET " +
                "is_read = TRUE, read_at = CURRENT_TIMESTAMP " +
                "WHERE user_id = ? AND is_read = FALSE";

        return jdbcTemplate.update(sql, userId);
    }

    // 6. Count unread notifications
    public int countUnreadNotifications(Long userId) {

        String sql = "SELECT COUNT(*) FROM notifications " +
                "WHERE user_id = ? AND is_read = FALSE";

        Integer count = jdbcTemplate.queryForObject(
                sql, Integer.class, userId
        );

        return count != null ? count : 0;
    }

    // 7. Delete a notification
    public boolean deleteNotification(Long notificationId) {

        String sql = "DELETE FROM notifications " +
                "WHERE notification_id = ?";

        int rows = jdbcTemplate.update(sql, notificationId);

        return rows > 0;
    }
}
