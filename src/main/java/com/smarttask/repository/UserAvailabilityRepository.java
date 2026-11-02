package com.smarttask.repository;

import com.smarttask.model.UserAvailability;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Repository
public class UserAvailabilityRepository 
{
    private final JdbcTemplate jdbcTemplate;

    // Constructor Injection
    public UserAvailabilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Maps database records to UserAvailability objects
    private final RowMapper<UserAvailability> availabilityRowMapper =
            (rs, rowNum) -> {

        UserAvailability availability = new UserAvailability();

        availability.setAvailabilityId(
                rs.getLong("availability_id")
        );

        availability.setUserId(rs.getLong("user_id"));

        Date date = rs.getDate("availability_date");

        if (date != null) {
            availability.setAvailabilityDate(date.toLocalDate());
        }

        availability.setAvailableHours(
                rs.getBigDecimal("available_hours")
        );

        availability.setAvailable(
                rs.getBoolean("is_available")
        );

        availability.setRemarks(rs.getString("remarks"));

        Timestamp createdAt = rs.getTimestamp("created_at");

        if (createdAt != null) {
            availability.setCreatedAt(
                    createdAt.toLocalDateTime()
            );
        }

        return availability;
    };

    // 1. Insert user availability
    public UserAvailability saveAvailability(
            UserAvailability availability) {

        String sql = "INSERT INTO user_availability " +
                "(user_id, availability_date, available_hours, " +
                "is_available, remarks) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );

            ps.setLong(1, availability.getUserId());

            ps.setDate(
                    2, Date.valueOf(availability.getAvailabilityDate())
            );

            ps.setBigDecimal(3, availability.getAvailableHours());
            ps.setBoolean(4, availability.isAvailable());
            ps.setString(5, availability.getRemarks());

            return ps;

        }, keyHolder);

        if (keyHolder.getKey() != null) {
            availability.setAvailabilityId(
                    keyHolder.getKey().longValue()
            );
        }

        return availability;
    }

    // 2. Get availability for a particular user on a date
    public UserAvailability getAvailability(
            Long userId, LocalDate date) {

        String sql = "SELECT * FROM user_availability " +
                "WHERE user_id = ? AND availability_date = ?";

        List<UserAvailability> records = jdbcTemplate.query(
                sql,
                availabilityRowMapper,
                userId,
                Date.valueOf(date)
        );

        return records.isEmpty() ? null : records.get(0);
    }

    // 3. Get all availability records of a user
    public List<UserAvailability> getAvailabilityByUserId(
            Long userId) {

        String sql = "SELECT * FROM user_availability " +
                "WHERE user_id = ? ORDER BY availability_date";

        return jdbcTemplate.query(
                sql, availabilityRowMapper, userId
        );
    }

    // 4. Get availability within a date range
    public List<UserAvailability> getAvailabilityBetweenDates(
            Long userId, LocalDate startDate, LocalDate endDate) {

        String sql = "SELECT * FROM user_availability " +
                "WHERE user_id = ? " +
                "AND availability_date BETWEEN ? AND ? " +
                "ORDER BY availability_date";

        return jdbcTemplate.query(
                sql,
                availabilityRowMapper,
                userId,
                Date.valueOf(startDate),
                Date.valueOf(endDate)
        );
    }

    // 5. Update availability
    public boolean updateAvailability(
            UserAvailability availability) {

        String sql = "UPDATE user_availability SET " +
                "available_hours = ?, is_available = ?, remarks = ? " +
                "WHERE user_id = ? AND availability_date = ?";

        int rows = jdbcTemplate.update(
                sql,
                availability.getAvailableHours(),
                availability.isAvailable(),
                availability.getRemarks(),
                availability.getUserId(),
                Date.valueOf(availability.getAvailabilityDate())
        );

        return rows > 0;
    }

    // 6. Delete availability record
    public boolean deleteAvailability(
            Long userId, LocalDate date) {

        String sql = "DELETE FROM user_availability " +
                "WHERE user_id = ? AND availability_date = ?";

        int rows = jdbcTemplate.update(
                sql, userId, Date.valueOf(date)
        );

        return rows > 0;
    }
}
