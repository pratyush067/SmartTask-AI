package com.smarttask.controller;

import com.smarttask.model.UserAvailability;
import com.smarttask.service.UserAvailabilityService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
public class UserAvailabilityController
{
    private final UserAvailabilityService availabilityService;

    public UserAvailabilityController(
            UserAvailabilityService availabilityService) {

        this.availabilityService = availabilityService;
    }

    // Add or update availability
    @PostMapping
    public ResponseEntity<UserAvailability> saveAvailability(
            @RequestBody UserAvailability availability) {

        UserAvailability saved =
            availabilityService.saveAvailability(availability);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Get availability for a particular user and date
    @GetMapping("/{userId}")
    public ResponseEntity<UserAvailability> getAvailability(
            @PathVariable Long userId,
            @RequestParam LocalDate date) {

        return ResponseEntity.ok(
            availabilityService.getAvailability(userId, date)
        );
    }

    // Get all availability records of a user
    @GetMapping("/{userId}/all")
    public ResponseEntity<List<UserAvailability>> getAvailabilityByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
            availabilityService.getAvailabilityByUserId(userId)
        );
    }

    // Get availability within a date range
    @GetMapping("/{userId}/range")
    public ResponseEntity<List<UserAvailability>> getAvailabilityBetweenDates(
            @PathVariable Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        return ResponseEntity.ok(
            availabilityService.getAvailabilityBetweenDates(
                userId, startDate, endDate
            )
        );
    }

    // Delete availability for a particular user and date
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteAvailability(
            @PathVariable Long userId,
            @RequestParam LocalDate date) {

        availabilityService.deleteAvailability(userId, date);

        return ResponseEntity.ok(
            "Availability deleted successfully."
        );
    }
}
