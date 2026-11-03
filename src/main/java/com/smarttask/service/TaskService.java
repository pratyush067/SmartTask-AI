package com.smarttask.service;

import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.repository.TaskRepository;
import com.smarttask.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            UserService userService) {

        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // 1. Create a task
    public Task createTask(Task task, User actor) {

        validateActor(actor);

        if (!"ADMIN".equals(actor.getRole()) &&
                !"SUPER_ADMIN".equals(actor.getRole())) {
            throw new IllegalArgumentException(
                    "You are not authorized to create tasks."
            );
        }

        validateTaskDetails(task);

        task.setCreatedBy(actor.getUserId());
        task.setStatus("PENDING");
        task.setProgressPercent(0);

        if (task.getPriority() == null || task.getPriority().isBlank()) {
            task.setPriority("MEDIUM");
        } else {
            task.setPriority(task.getPriority().toUpperCase());
        }

        if (task.getAssignedTo() != null) {
            validateAssignee(task.getAssignedTo(), actor);
        }

        return taskRepository.saveTask(task);
    }

    // 2. Get all tasks accessible to the logged-in user
    public List<Task> getAllTasks(User actor) {

        validateActor(actor);

        if ("SUPER_ADMIN".equals(actor.getRole())) {
            return taskRepository.getAllTasks();
        }

        if ("ADMIN".equals(actor.getRole())) {
            return taskRepository.getTasksCreatedBy(actor.getUserId());
        }

        if ("USER".equals(actor.getRole())) {
            return taskRepository.getTasksAssignedToUser(
                    actor.getUserId()
            );
        }

        throw new IllegalArgumentException("Invalid user role.");
    }

    // 3. Get a task by ID after checking access
    public Task getTaskById(Long taskId, User actor) {

        validateActor(actor);

        Task task = findTask(taskId);
        checkTaskAccess(task, actor);

        return task;
    }

    // 4. Get tasks assigned to a particular user
    public List<Task> getTasksAssignedToUser(
            Long userId, User actor) {

        validateActor(actor);

        if ("USER".equals(actor.getRole()) &&
                !actor.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "You can only view your own assigned tasks."
            );
        }

        if ("ADMIN".equals(actor.getRole())) {
            List<User> team = userService.getUsersByAdminId(
                    actor.getUserId()
            );

            boolean isTeamMember = team.stream()
                    .anyMatch(user -> user.getUserId().equals(userId));

            if (!isTeamMember) {
                throw new IllegalArgumentException(
                        "You can only view tasks assigned to your team."
                );
            }
        }

        User user = userRepository.getUserById(userId);

        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        return taskRepository.getTasksAssignedToUser(userId);
    }

    // 5. Update task details (Admin or Super Admin only)
    public Task updateTask(
            Long taskId, Task updatedTask, User actor) {

        validateActor(actor);

        if (!"ADMIN".equals(actor.getRole()) &&
                !"SUPER_ADMIN".equals(actor.getRole())) {
            throw new IllegalArgumentException(
                    "Users cannot edit task details."
            );
        }

        Task existingTask = findTask(taskId);
        checkTaskAccess(existingTask, actor);

        validateTaskDetails(updatedTask);

        if (updatedTask.getAssignedTo() != null) {
            validateAssignee(updatedTask.getAssignedTo(), actor);
        }

        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        existingTask.setAssignedTo(updatedTask.getAssignedTo());
        existingTask.setPriority(
                updatedTask.getPriority() == null
                        ? existingTask.getPriority()
                        : updatedTask.getPriority().toUpperCase()
        );
        existingTask.setDeadline(updatedTask.getDeadline());
        existingTask.setEstimatedHours(updatedTask.getEstimatedHours());
        existingTask.setActualHours(updatedTask.getActualHours());

        if (updatedTask.getStatus() != null) {
            validateStatus(updatedTask.getStatus());
            existingTask.setStatus(updatedTask.getStatus().toUpperCase());
        }

        if (updatedTask.getProgressPercent() != null) {
            validateProgress(updatedTask.getProgressPercent());
            existingTask.setProgressPercent(
                    updatedTask.getProgressPercent()
            );
        }

        boolean updated = taskRepository.updateTask(existingTask);

        if (!updated) {
            throw new IllegalArgumentException("Unable to update task.");
        }

        return findTask(taskId);
    }

    // 6. Update task status
    public Task updateTaskStatus(
            Long taskId, String status, User actor) {

        validateActor(actor);

        Task task = findTask(taskId);
        checkTaskAccess(task, actor);

        validateStatus(status);
        status = status.toUpperCase();

        if ("USER".equals(actor.getRole()) &&
                !Arrays.asList("IN_PROGRESS", "COMPLETED").contains(status)) {
            throw new IllegalArgumentException(
                    "Users can only mark tasks as IN_PROGRESS or COMPLETED."
            );
        }

        boolean updated = taskRepository.updateTaskStatus(taskId, status);

        if (!updated) {
            throw new IllegalArgumentException(
                    "Unable to update task status."
            );
        }

        return findTask(taskId);
    }

    // 7. Update task progress
    public Task updateTaskProgress(
            Long taskId, Integer progress, User actor) {

        validateActor(actor);

        Task task = findTask(taskId);
        checkTaskAccess(task, actor);
        validateProgress(progress);

        if ("USER".equals(actor.getRole()) &&
                "CANCELLED".equals(task.getStatus())) {
            throw new IllegalArgumentException(
                    "Cancelled tasks cannot be updated."
            );
        }

        boolean updated = taskRepository.updateTaskProgress(
                taskId, progress
        );

        if (!updated) {
            throw new IllegalArgumentException(
                    "Unable to update task progress."
            );
        }

        return findTask(taskId);
    }

    // 8. Cancel a task (Admin or Super Admin only)
    public Task cancelTask(Long taskId, User actor) {

        validateActor(actor);

        if (!"ADMIN".equals(actor.getRole()) &&
                !"SUPER_ADMIN".equals(actor.getRole())) {
            throw new IllegalArgumentException(
                    "You are not authorized to cancel tasks."
            );
        }

        Task task = findTask(taskId);
        checkTaskAccess(task, actor);

        boolean updated = taskRepository.updateTaskStatus(
                taskId, "CANCELLED"
        );

        if (!updated) {
            throw new IllegalArgumentException(
                    "Unable to cancel task."
            );
        }

        return findTask(taskId);
    }

    // Find task without exposing it before access validation
    private Task findTask(Long taskId) {

        Task task = taskRepository.getTaskById(taskId);

        if (task == null) {
            throw new IllegalArgumentException("Task not found.");
        }

        return task;
    }

    // Check whether the logged-in user can access a task
    private void checkTaskAccess(Task task, User actor) {

        if ("SUPER_ADMIN".equals(actor.getRole())) {
            return;
        }

        if ("ADMIN".equals(actor.getRole()) &&
                task.getCreatedBy().equals(actor.getUserId())) {
            return;
        }

        if ("USER".equals(actor.getRole()) &&
                task.getAssignedTo() != null &&
                task.getAssignedTo().equals(actor.getUserId())) {
            return;
        }

        throw new IllegalArgumentException(
                "You are not authorized to access this task."
        );
    }

    // Validate the logged-in user
    private void validateActor(User actor) {

        if (actor == null || !actor.isActive()) {
            throw new IllegalArgumentException(
                    "Invalid or inactive user."
            );
        }
    }

    // Validate task details
    private void validateTaskDetails(Task task) {

        if (task == null) {
            throw new IllegalArgumentException(
                    "Task details are required."
            );
        }

        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title is required.");
        }

        if (task.getTitle().length() > 255) {
            throw new IllegalArgumentException(
                    "Task title cannot exceed 255 characters."
            );
        }

        if (task.getDeadline() != null &&
                task.getDeadline().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Deadline cannot be in the past."
            );
        }

        if (task.getEstimatedHours() != null &&
                task.getEstimatedHours().signum() < 0) {
            throw new IllegalArgumentException(
                    "Estimated hours cannot be negative."
            );
        }

        if (task.getActualHours() != null &&
                task.getActualHours().signum() < 0) {
            throw new IllegalArgumentException(
                    "Actual hours cannot be negative."
            );
        }

        if (task.getPriority() != null &&
                !task.getPriority().isBlank()) {
            validatePriority(task.getPriority());
        }
    }

    // Validate assigned user and team membership
    private void validateAssignee(Long userId, User actor) {

        User user = userRepository.getUserById(userId);

        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException(
                    "Invalid or inactive assigned user."
            );
        }

        if (!"USER".equals(user.getRole())) {
            throw new IllegalArgumentException(
                    "Tasks can only be assigned to regular users."
            );
        }

        if ("ADMIN".equals(actor.getRole())) {

            List<User> team = userService.getUsersByAdminId(
                    actor.getUserId()
            );

            boolean isTeamMember = team.stream()
                    .anyMatch(member -> member.getUserId().equals(userId));

            if (!isTeamMember) {
                throw new IllegalArgumentException(
                        "You can only assign tasks to your own team members."
                );
            }
        }
    }

    // Validate task status
    private void validateStatus(String status) {

        List<String> validStatuses = Arrays.asList(
                "PENDING",
                "IN_PROGRESS",
                "COMPLETED",
                "CANCELLED"
        );

        if (status == null ||
                !validStatuses.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("Invalid task status.");
        }
    }

    // Validate task priority
    private void validatePriority(String priority) {

        List<String> validPriorities = Arrays.asList(
                "LOW",
                "MEDIUM",
                "HIGH",
                "URGENT"
        );

        if (priority == null ||
                !validPriorities.contains(priority.toUpperCase())) {
            throw new IllegalArgumentException("Invalid task priority.");
        }
    }

    // Validate progress percentage
    private void validateProgress(Integer progress) {

        if (progress == null || progress < 0 || progress > 100) {
            throw new IllegalArgumentException(
                    "Progress must be between 0 and 100."
            );
        }
    }
}