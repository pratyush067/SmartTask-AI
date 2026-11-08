
package com.smarttask.controller;

import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.service.TaskService;
import com.smarttask.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final UserService userService;

    public TaskController(
            TaskService taskService,
            UserService userService) {

        this.taskService = taskService;
        this.userService = userService;
    }

    // Get the currently logged-in user
    private User getLoggedInUser(Authentication authentication) {

        if (authentication == null) {
            throw new IllegalArgumentException(
                    "Authentication required."
            );
        }

        User user = userService.getUserByEmail(authentication.getName());

        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException(
                    "Invalid or inactive user."
            );
        }

        return user;
    }

    // Handle error responses
    private ResponseEntity<?> errorResponse(
            IllegalArgumentException e) {

        String message = e.getMessage();

        if ("Task not found.".equals(message)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", message));
        }

        if (message != null &&
                (message.contains("not authorized") ||
                 message.contains("only view") ||
                 message.contains("only assign") ||
                 message.contains("cannot edit") ||
                 message.contains("only mark") ||
                 message.contains("cannot cancel"))) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", message));
        }

        return ResponseEntity.badRequest()
                .body(Map.of("message",
                        message == null ? "Invalid request." : message));
    }

    // 1. Create a task
    @PostMapping
    public ResponseEntity<?> createTask(
            @RequestBody Task task,
            Authentication authentication) {

        try {
            User actor = getLoggedInUser(authentication);

            Task savedTask = taskService.createTask(task, actor);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(savedTask);

        } catch (IllegalArgumentException e) {
            return errorResponse(e);
        }
    }

    // 2. Get all accessible tasks
    @GetMapping
    public ResponseEntity<?> getAllTasks(
            Authentication authentication) {

        try {
            User actor = getLoggedInUser(authentication);

            List<Task> tasks = taskService.getAllTasks(actor);

            return ResponseEntity.ok(tasks);

        } catch (IllegalArgumentException e) {
            return errorResponse(e);
        }
    }

    // 3. Get tasks assigned to the logged-in Admin's team
    @GetMapping("/team")
    public ResponseEntity<?> getTeamTasks(
            Authentication authentication) {

        try {
            User actor = getLoggedInUser(authentication);

            List<Task> tasks = taskService.getTeamTasks(actor);

            return ResponseEntity.ok(tasks);

        } catch (IllegalArgumentException e) {
            return errorResponse(e);
        }
    }

    // 4. Get a task by ID
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskById(
            @PathVariable Long taskId,
            Authentication authentication) {

        try {
            User actor = getLoggedInUser(authentication);

            return ResponseEntity.ok(
                    taskService.getTaskById(taskId, actor)
            );

        } catch (IllegalArgumentException e) {
            return errorResponse(e);
        }
    }

    // 5. Get tasks assigned to a user
    @GetMapping("/assigned/{userId}")
    public ResponseEntity<?> getTasksAssignedToUser(
            @PathVariable Long userId,
            Authentication authentication) {

        try {
            User actor = getLoggedInUser(authentication);

            return ResponseEntity.ok(
                    taskService.getTasksAssignedToUser(userId, actor)
            );

        } catch (IllegalArgumentException e) {
            return errorResponse(e);
        }
    }

    // 6. Update task details
    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(
            @PathVariable Long taskId,
            @RequestBody Task task,
            Authentication authentication) {

        try {
            User actor = getLoggedInUser(authentication);

            return ResponseEntity.ok(
                    taskService.updateTask(taskId, task, actor)
            );

        } catch (IllegalArgumentException e) {
            return errorResponse(e);
        }
    }

    // 7. Update task status
    @PutMapping("/{taskId}/status")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            User actor = getLoggedInUser(authentication);

            String status = request.get("status");

            return ResponseEntity.ok(
                    taskService.updateTaskStatus(taskId, status, actor)
            );

        } catch (IllegalArgumentException e) {
            return errorResponse(e);
        }
    }

    // 8. Update task progress
    @PutMapping("/{taskId}/progress")
    public ResponseEntity<?> updateTaskProgress(
            @PathVariable Long taskId,
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {

        try {
            User actor = getLoggedInUser(authentication);

            Integer progress = request.get("progressPercent");

            return ResponseEntity.ok(
                    taskService.updateTaskProgress(taskId, progress, actor)
            );

        } catch (IllegalArgumentException e) {
            return errorResponse(e);
        }
    }

    // 9. Cancel a task
    @PutMapping("/{taskId}/cancel")
    public ResponseEntity<?> cancelTask(
            @PathVariable Long taskId,
            Authentication authentication) {

        try {
            User actor = getLoggedInUser(authentication);

            return ResponseEntity.ok(
                    taskService.cancelTask(taskId, actor)
            );

        } catch (IllegalArgumentException e) {
            return errorResponse(e);
        }
    }
}
