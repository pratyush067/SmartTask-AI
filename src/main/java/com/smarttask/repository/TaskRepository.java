package com.smarttask.repository;

import com.smarttask.model.Task;

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
public class TaskRepository 
{
    private final JdbcTemplate jdbcTemplate;

    // Constructor Injection
    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Maps database records to Task objects
    private final RowMapper<Task> taskRowMapper = (rs, rowNum) -> {

        Task task = new Task();

        task.setTaskId(rs.getLong("task_id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setCreatedBy(rs.getLong("created_by"));

        long assignedTo = rs.getLong("assigned_to");
        if (!rs.wasNull()) {
            task.setAssignedTo(assignedTo);
        }

        task.setStatus(rs.getString("status"));
        task.setPriority(rs.getString("priority"));

        Timestamp deadline = rs.getTimestamp("deadline");
        if (deadline != null) {
            task.setDeadline(deadline.toLocalDateTime());
        }

        task.setEstimatedHours(rs.getBigDecimal("estimated_hours"));
        task.setActualHours(rs.getBigDecimal("actual_hours"));
        task.setProgressPercent(rs.getInt("progress_percent"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            task.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            task.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return task;
    };

    // 1. Insert a new task
    public Task saveTask(Task task) {

        String sql = "INSERT INTO tasks " +
                "(title, description, created_by, assigned_to, " +
                "status, priority, deadline, estimated_hours) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setLong(3, task.getCreatedBy());
            ps.setObject(4, task.getAssignedTo());
            ps.setString(5, task.getStatus());
            ps.setString(6, task.getPriority());
            ps.setObject(7, task.getDeadline());
            ps.setBigDecimal(8, task.getEstimatedHours());

            return ps;

        }, keyHolder);

        if (keyHolder.getKey() != null) {
            task.setTaskId(keyHolder.getKey().longValue());
        }

        return task;
    }

    // 2. Find task by ID
    public Task getTaskById(Long taskId) {

        String sql = "SELECT * FROM tasks WHERE task_id = ?";

        List<Task> tasks = jdbcTemplate.query(
                sql, taskRowMapper, taskId
        );

        return tasks.isEmpty() ? null : tasks.get(0);
    }

    // 3. Retrieve all tasks
    public List<Task> getAllTasks() {

        String sql = "SELECT * FROM tasks ORDER BY task_id";

        return jdbcTemplate.query(sql, taskRowMapper);
    }

    // 4. Retrieve tasks assigned to a user
    public List<Task> getTasksAssignedToUser(Long userId) {

        String sql = "SELECT * FROM tasks " +
                "WHERE assigned_to = ? ORDER BY deadline";

        return jdbcTemplate.query(
                sql, taskRowMapper, userId
        );
    }

    // 5. Retrieve tasks created by an admin
    public List<Task> getTasksCreatedBy(Long adminId) {

        String sql = "SELECT * FROM tasks " +
                "WHERE created_by = ? ORDER BY created_at DESC";

        return jdbcTemplate.query(
                sql, taskRowMapper, adminId
        );
    }

    // 6. Update task details
    public boolean updateTask(Task task) {

        String sql = "UPDATE tasks SET " +
                "title = ?, description = ?, assigned_to = ?, " +
                "status = ?, priority = ?, deadline = ?, " +
                "estimated_hours = ?, actual_hours = ?, " +
                "progress_percent = ? " +
                "WHERE task_id = ?";

        int rows = jdbcTemplate.update(
                sql,
                task.getTitle(),
                task.getDescription(),
                task.getAssignedTo(),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline(),
                task.getEstimatedHours(),
                task.getActualHours(),
                task.getProgressPercent(),
                task.getTaskId()
        );

        return rows > 0;
    }

    // 7. Update task status
    public boolean updateTaskStatus(Long taskId, String status) {

        String sql = "UPDATE tasks SET status = ? WHERE task_id = ?";

        int rows = jdbcTemplate.update(sql, status, taskId);

        return rows > 0;
    }

    // 8. Update task progress
    public boolean updateTaskProgress(Long taskId, Integer progress) {

        String sql = "UPDATE tasks SET progress_percent = ? " +
                "WHERE task_id = ?";

        int rows = jdbcTemplate.update(sql, progress, taskId);

        return rows > 0;
    }
}
