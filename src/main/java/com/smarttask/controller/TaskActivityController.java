package com.smarttask.controller;

import com.smarttask.model.TaskActivity;
import com.smarttask.service.TaskActivityService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class TaskActivityController
{
    private final TaskActivityService activityService;

    public TaskActivityController(TaskActivityService activityService) {
        this.activityService = activityService;
    }

    // Record a new activity
    @PostMapping
    public ResponseEntity<TaskActivity> recordActivity(
            @RequestBody TaskActivity activity) {

        TaskActivity savedActivity =
            activityService.recordActivity(activity);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(savedActivity);
    }

    // Get activities related to a particular task
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskActivity>> getActivitiesByTaskId(
            @PathVariable Long taskId) {

        return ResponseEntity.ok(
            activityService.getActivitiesByTaskId(taskId)
        );
    }

    // Get activities performed by a particular user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskActivity>> getActivitiesByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
            activityService.getActivitiesByUserId(userId)
        );
    }

    // Get all recorded activities
    @GetMapping
    public ResponseEntity<List<TaskActivity>> getAllActivities() {

        return ResponseEntity.ok(
            activityService.getAllActivities()
        );
    }
}
