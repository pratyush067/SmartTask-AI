package com.smarttask.controller;

import com.smarttask.model.Task;
import com.smarttask.service.TaskService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController
{
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // Create a new task
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {

        Task savedTask = taskService.createTask(task);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
    }

    // Get all tasks
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {

        return ResponseEntity.ok(taskService.getAllTasks());
    }

    // Get a task by ID
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {

        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    // Get tasks assigned to a user
    @GetMapping("/assigned/{userId}")
    public ResponseEntity<List<Task>> getTasksAssignedToUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
            taskService.getTasksAssignedToUser(userId)
        );
    }

    // Get tasks created by a user
    @GetMapping("/created/{userId}")
    public ResponseEntity<List<Task>> getTasksCreatedBy(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
            taskService.getTasksCreatedBy(userId)
        );
    }

    // Update task details
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @RequestBody Task task) {

        task.setTaskId(id);

        return ResponseEntity.ok(taskService.updateTask(task));
    }

    // Update task status
    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateTaskStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam Long performedBy) {

        // Correct order: taskId, performedBy, status
        taskService.updateTaskStatus(id, performedBy, status);

        return ResponseEntity.ok(
            "Task status updated successfully."
        );
    }

    // Update task progress
    @PutMapping("/{id}/progress")
    public ResponseEntity<String> updateTaskProgress(
            @PathVariable Long id,
            @RequestParam int progress,
            @RequestParam Long performedBy) {

        // Correct order: taskId, performedBy, progress
        taskService.updateTaskProgress(id, performedBy, progress);

        return ResponseEntity.ok(
            "Task progress updated successfully."
        );
    }

    // Cancel a task
    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelTask(
            @PathVariable Long id,
            @RequestParam Long performedBy) {

        taskService.cancelTask(id, performedBy);

        return ResponseEntity.ok(
            "Task cancelled successfully."
        );
    }
}