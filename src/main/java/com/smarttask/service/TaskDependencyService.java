package com.smarttask.service;

import com.smarttask.model.Task;
import com.smarttask.model.TaskDependency;
import com.smarttask.model.User;

import com.smarttask.repository.TaskRepository;
import com.smarttask.repository.TaskDependencyRepository;
import com.smarttask.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;

@Service
public class TaskDependencyService 
{
    private final TaskDependencyRepository dependencyRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // Constructor Injection
    public TaskDependencyService(
            TaskDependencyRepository dependencyRepository,
            TaskRepository taskRepository,
            UserRepository userRepository) {

        this.dependencyRepository = dependencyRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    // Validate whether a task exists
    private Task validateTask(Long taskId) {

        Task task = taskRepository.getTaskById(taskId);

        if (task == null) {
            throw new IllegalArgumentException(
                    "Task with ID " + taskId + " does not exist"
            );
        }

        return task;
    }

    // Validate whether a user exists and is active
    private void validateActiveUser(Long userId) {

        User user = userRepository.getUserById(userId);

        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException(
                    "User does not exist or is inactive"
            );
        }
    }

    // 1. Check whether adding a dependency creates a cycle
    private boolean createsCycle(
            Long taskId, Long dependsOnTaskId) {

        Queue<Long> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();

        queue.add(dependsOnTaskId);

        while (!queue.isEmpty()) {

            Long currentTaskId = queue.poll();

            if (currentTaskId.equals(taskId)) {
                return true;
            }

            if (visited.contains(currentTaskId)) {
                continue;
            }

            visited.add(currentTaskId);

            // Get tasks that the current task depends on
            List<TaskDependency> dependencies =
                    dependencyRepository.getDependenciesForTask(
                            currentTaskId
                    );

            for (TaskDependency dependency : dependencies) {

                Long nextTaskId = dependency.getDependsOnTaskId();

                if (!visited.contains(nextTaskId)) {
                    queue.add(nextTaskId);
                }
            }
        }

        return false;
    }

    // 2. Add a dependency between two tasks
    public TaskDependency addDependency(
            Long taskId,
            Long dependsOnTaskId,
            Long performedBy) {

        validateTask(taskId);
        validateTask(dependsOnTaskId);
        validateActiveUser(performedBy);

        // A task cannot depend on itself
        if (taskId.equals(dependsOnTaskId)) {
            throw new IllegalArgumentException(
                    "A task cannot depend on itself"
            );
        }

        // Check for duplicate dependency
        if (dependencyRepository.dependencyExists(
                taskId, dependsOnTaskId)) {

            throw new IllegalArgumentException(
                    "Dependency already exists"
            );
        }

        // Prevent circular dependencies
        if (createsCycle(taskId, dependsOnTaskId)) {

            throw new IllegalArgumentException(
                    "Circular dependency detected"
            );
        }

        TaskDependency dependency = new TaskDependency(
                taskId, dependsOnTaskId
        );

        return dependencyRepository.saveDependency(dependency);
    }

    // 3. Get all dependencies of a task
    public List<TaskDependency> getDependenciesForTask(
            Long taskId) {

        validateTask(taskId);

        return dependencyRepository.getDependenciesForTask(taskId);
    }

    // 4. Get tasks that depend on a particular task
    public List<TaskDependency> getTasksDependingOn(
            Long taskId) {

        validateTask(taskId);

        return dependencyRepository.getTasksDependingOn(taskId);
    }

    // 5. Remove a dependency
    public boolean removeDependency(
            Long taskId, Long dependsOnTaskId) {

        validateTask(taskId);
        validateTask(dependsOnTaskId);

        return dependencyRepository.deleteDependency(
                taskId, dependsOnTaskId
        );
    }

    // 6. Check whether all prerequisites are completed
    public boolean areDependenciesCompleted(Long taskId) {

        validateTask(taskId);

        List<TaskDependency> dependencies =
                dependencyRepository.getDependenciesForTask(taskId);

        for (TaskDependency dependency : dependencies) {

            Task prerequisite = taskRepository.getTaskById(
                    dependency.getDependsOnTaskId()
            );

            if (prerequisite == null ||
                    !prerequisite.getStatus().equals("COMPLETED")) {

                return false;
            }
        }

        return true;
    }
}
