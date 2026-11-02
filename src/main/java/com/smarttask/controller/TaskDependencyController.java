package com.smarttask.controller;

import com.smarttask.model.TaskDependency;
import com.smarttask.service.TaskDependencyService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskDependencyController
{
    private final TaskDependencyService dependencyService;

    public TaskDependencyController(
            TaskDependencyService dependencyService) {

        this.dependencyService = dependencyService;
    }

    // Add a dependency to a task
    @PostMapping("/{taskId}/dependencies")
    public ResponseEntity<TaskDependency> addDependency(
            @PathVariable Long taskId,
            @RequestParam Long dependsOnTaskId,
            @RequestParam Long performedBy) {

        TaskDependency dependency = dependencyService.addDependency(
                taskId, dependsOnTaskId, performedBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(dependency);
    }

    // Get all prerequisites of a task
    @GetMapping("/{taskId}/dependencies")
    public ResponseEntity<List<TaskDependency>> getDependenciesForTask(
            @PathVariable Long taskId) {

        return ResponseEntity.ok(
                dependencyService.getDependenciesForTask(taskId));
    }

    // Get all tasks that depend on a particular task
    @GetMapping("/{taskId}/dependents")
    public ResponseEntity<List<TaskDependency>> getTasksDependingOn(
            @PathVariable Long taskId) {

        return ResponseEntity.ok(
                dependencyService.getTasksDependingOn(taskId));
    }

    // Check whether all dependencies are completed
    @GetMapping("/{taskId}/dependencies/completed")
    public ResponseEntity<Boolean> areDependenciesCompleted(
            @PathVariable Long taskId) {

        boolean completed = dependencyService.areDependenciesCompleted(taskId);

        return ResponseEntity.ok(completed);
    }

    // Remove a dependency
    @DeleteMapping("/{taskId}/dependencies/{dependsOnTaskId}")
    public ResponseEntity<String> removeDependency(
            @PathVariable Long taskId,
            @PathVariable Long dependsOnTaskId) {

        boolean removed = dependencyService.removeDependency(
                taskId, dependsOnTaskId);

        if (removed) {
            return ResponseEntity.ok(
                    "Task dependency removed successfully.");
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Task dependency not found.");
    }
}