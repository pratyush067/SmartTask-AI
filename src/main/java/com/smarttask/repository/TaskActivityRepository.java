package com.smarttask.repository;

import com.smarttask.model.TaskActivity;

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
public class TaskActivityRepository 
{
    private final JdbcTemplate jdbcTemplate;

    // Constructor Injection
    public TaskActivityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Maps database records to TaskActivity objects
    private final RowMapper<TaskActivity> activityRowMapper =
            (rs, rowNum) -> {

        TaskActivity activity = new TaskActivity();

        activity.setActivityId(rs.getLong("activity_id"));
        activity.setTaskId(rs.getLong("task_id"));
        activity.setPerformedBy(rs.getLong("performed_by"));
        activity.setActivityType(rs.getString("activity_type"));
        activity.setOldValue(rs.getString("old_value"));
        activity.setNewValue(rs.getString("new_value"));
        activity.setRemarks(rs.getString("remarks"));

        Timestamp createdAt = rs.getTimestamp("created_at");

        if (createdAt != null) {
            activity.setCreatedAt(createdAt.toLocalDateTime());
        }

        return activity;
    };

    // 1. Record a new task activity
    public TaskActivity saveActivity(TaskActivity activity) {

        String sql = "INSERT INTO task_activity " +
                "(task_id, performed_by, activity_type, " +
                "old_value, new_value, remarks) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );

            ps.setLong(1, activity.getTaskId());
            ps.setLong(2, activity.getPerformedBy());
            ps.setString(3, activity.getActivityType());
            ps.setString(4, activity.getOldValue());
            ps.setString(5, activity.getNewValue());
            ps.setString(6, activity.getRemarks());

            return ps;

        }, keyHolder);

        if (keyHolder.getKey() != null) {
            activity.setActivityId(
                    keyHolder.getKey().longValue()
            );
        }

        return activity;
    }

    // 2. Get complete activity history of a task
    public List<TaskActivity> getActivitiesByTaskId(Long taskId) {

        String sql = "SELECT * FROM task_activity " +
                "WHERE task_id = ? ORDER BY created_at DESC";

        return jdbcTemplate.query(
                sql, activityRowMapper, taskId
        );
    }

    // 3. Get activities performed by a particular user
    public List<TaskActivity> getActivitiesByUserId(Long userId) {

        String sql = "SELECT * FROM task_activity " +
                "WHERE performed_by = ? ORDER BY created_at DESC";

        return jdbcTemplate.query(
                sql, activityRowMapper, userId
        );
    }

    // 4. Get all task activities
    public List<TaskActivity> getAllActivities() {

        String sql = "SELECT * FROM task_activity " +
                "ORDER BY created_at DESC";

        return jdbcTemplate.query(sql, activityRowMapper);
    }
}