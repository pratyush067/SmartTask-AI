package com.smarttask.repository;

import com.smarttask.model.TaskDependency;

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
public class TaskDependencyRepository 
{
    private final JdbcTemplate jdbcTemplate;

    // Constructor Injection
    public TaskDependencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Maps database records to TaskDependency objects
    private final RowMapper<TaskDependency> dependencyRowMapper =
            (rs, rowNum) -> {

        TaskDependency dependency = new TaskDependency();

        dependency.setDependencyId(rs.getLong("dependency_id"));
        dependency.setTaskId(rs.getLong("task_id"));
        dependency.setDependsOnTaskId(
                rs.getLong("depends_on_task_id")
        );

        Timestamp createdAt = rs.getTimestamp("created_at");

        if (createdAt != null) {
            dependency.setCreatedAt(createdAt.toLocalDateTime());
        }

        return dependency;
    };

    // 1. Add a dependency between two tasks
    public TaskDependency saveDependency(TaskDependency dependency) {

        String sql = "INSERT INTO task_dependencies " +
                "(task_id, depends_on_task_id) VALUES (?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );

            ps.setLong(1, dependency.getTaskId());
            ps.setLong(2, dependency.getDependsOnTaskId());

            return ps;

        }, keyHolder);

        if (keyHolder.getKey() != null) {
            dependency.setDependencyId(
                    keyHolder.getKey().longValue()
            );
        }

        return dependency;
    }

    // 2. Get all dependencies of a particular task
    public List<TaskDependency> getDependenciesForTask(Long taskId) {

        String sql = "SELECT * FROM task_dependencies " +
                "WHERE task_id = ? ORDER BY dependency_id";

        return jdbcTemplate.query(
                sql, dependencyRowMapper, taskId
        );
    }

    // 3. Get tasks that depend on a particular task
    public List<TaskDependency> getTasksDependingOn(Long taskId) {

        String sql = "SELECT * FROM task_dependencies " +
                "WHERE depends_on_task_id = ? ORDER BY dependency_id";

        return jdbcTemplate.query(
                sql, dependencyRowMapper, taskId
        );
    }

    // 4. Remove a dependency
    public boolean deleteDependency(Long taskId, Long dependsOnTaskId) {

        String sql = "DELETE FROM task_dependencies " +
                "WHERE task_id = ? AND depends_on_task_id = ?";

        int rows = jdbcTemplate.update(
                sql, taskId, dependsOnTaskId
        );

        return rows > 0;
    }

    // 5. Check whether a dependency already exists
    public boolean dependencyExists(
            Long taskId, Long dependsOnTaskId) {

        String sql = "SELECT COUNT(*) FROM task_dependencies " +
                "WHERE task_id = ? AND depends_on_task_id = ?";

        Integer count = jdbcTemplate.queryForObject(
                sql, Integer.class, taskId, dependsOnTaskId
        );

        return count != null && count > 0;
    }
}
