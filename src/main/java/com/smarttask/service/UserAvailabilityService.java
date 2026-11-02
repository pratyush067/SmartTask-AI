package com.smarttask.service;

import com.smarttask.model.User;
import com.smarttask.model.UserAvailability;
import com.smarttask.repository.UserAvailabilityRepository;
import com.smarttask.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class UserAvailabilityService 
{
    private final UserAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;

    public UserAvailabilityService(
            UserAvailabilityRepository availabilityRepository,
            UserRepository userRepository) {

        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
    }

    // Validate whether a user exists and is active
    private void validateActiveUser(Long userId) {

        User user = userRepository.getUserById(userId);

        if (user == null || !user.isActive()) {
            throw new RuntimeException(
                "User does not exist or is inactive."
            );
        }
    }

    // Validate available hours
    private void validateHours(BigDecimal hours) {

        if (hours == null ||
            hours.compareTo(BigDecimal.ZERO) < 0 ||
            hours.compareTo(new BigDecimal("24")) > 0) {

            throw new IllegalArgumentException(
                "Available hours must be between 0 and 24."
            );
        }
    }

    // Add or update availability
    public UserAvailability saveAvailability(
            UserAvailability availability) {

        if (availability == null) {
            throw new IllegalArgumentException(
                "Availability cannot be null."
            );
        }

        validateActiveUser(availability.getUserId());

        if (availability.getAvailabilityDate() == null) {
            throw new IllegalArgumentException(
                "Availability date is required."
            );
        }

        validateHours(availability.getAvailableHours());

        UserAvailability existing =
            availabilityRepository.getAvailability(
                availability.getUserId(),
                availability.getAvailabilityDate()
            );

        if (existing != null) {

            availability.setAvailabilityId(
                existing.getAvailabilityId()
            );

            availabilityRepository.updateAvailability(availability);

        } else {

            availabilityRepository.saveAvailability(availability);
        }

        return availability;
    }

    // Get availability for a particular date
    public UserAvailability getAvailability(
            Long userId, LocalDate date) {

        validateActiveUser(userId);

        if (date == null) {
            throw new IllegalArgumentException(
                "Date is required."
            );
        }

        return availabilityRepository.getAvailability(userId, date);
    }

    // Get all availability records of a user
    public List<UserAvailability> getAvailabilityByUserId(
            Long userId) {

        validateActiveUser(userId);

        return availabilityRepository.getAvailabilityByUserId(userId);
    }

    // Get availability within a date range
    public List<UserAvailability> getAvailabilityBetweenDates(
            Long userId, LocalDate startDate, LocalDate endDate) {

        validateActiveUser(userId);

        if (startDate == null || endDate == null ||
            endDate.isBefore(startDate)) {

            throw new IllegalArgumentException(
                "Invalid date range."
            );
        }

        return availabilityRepository.getAvailabilityBetweenDates(
            userId, startDate, endDate
        );
    }

    // Delete availability for a particular user and date
    public void deleteAvailability(Long userId, LocalDate date) {

        validateActiveUser(userId);

        if (date == null) {
            throw new IllegalArgumentException(
                "Date is required."
            );
        }

        availabilityRepository.deleteAvailability(userId, date);
    }
}