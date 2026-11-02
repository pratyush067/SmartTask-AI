package com.smarttask.service;

import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.model.TaskActivity;
import com.smarttask.model.Notification;

import com.smarttask.repository.TaskRepository;
import com.smarttask.repository.UserRepository;
import com.smarttask.repository.TaskActivityRepository;
import com.smarttask.repository.NotificationRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TaskService 
{
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskActivityRepository activityRepository;
    private final NotificationRepository notificationRepository;

    // Constructor Injection
    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskActivityRepository activityRepository,
            NotificationRepository notificationRepository) {

        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.notificationRepository = notificationRepository;
    }

    // Validate whether a user exists and is active
    private User validateActiveUser(Long userId) {

        User user = userRepository.getUserById(userId);

        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException(
                    "User does not exist or is inactive"
            );
        }

        return user;
    }

    // Validate task status
    private boolean isValidStatus(String status) {

        return status != null &&
                (status.equals("PENDING") ||
                 status.equals("IN_PROGRESS") ||
                 status.equals("COMPLETED") ||
                 status.equals("CANCELLED"));
    }

    // Validate task priority
    private boolean isValidPriority(String priority) {

        return priority != null &&
                (priority.equals("LOW") ||
                 priority.equals("MEDIUM") ||
                 priority.equals("HIGH") ||
                 priority.equals("URGENT"));
    }

    // Record task activity
    private void recordActivity(
            Long taskId,
            Long performedBy,
            String activityType,
            String oldValue,
            String newValue,
            String remarks) {

        TaskActivity activity = new TaskActivity(
                taskId,
                performedBy,
                activityType,
                oldValue,
                newValue,
                remarks
        );

        activityRepository.saveActivity(activity);
    }

    // 1. Create a new task
    public Task createTask(Task task) {

        if (task.getTitle() == null ||
                task.getTitle().isBlank()) {
            throw new IllegalArgumentException(
                    "Task title cannot be empty"
            );
        }

        // Validate creator
        validateActiveUser(task.getCreatedBy());

        // Validate assigned user, if provided
        if (task.getAssignedTo() != null) {
            validateActiveUser(task.getAssignedTo());
        }

        // Set default status and priority
        if (task.getStatus() == null) {
            task.setStatus("PENDING");
        }

        if (task.getPriority() == null) {
            task.setPriority("MEDIUM");
        }

        if (!isValidStatus(task.getStatus())) {
            throw new IllegalArgumentException(
                    "Invalid task status"
            );
        }

        if (!isValidPriority(task.getPriority())) {
            throw new IllegalArgumentException(
                    "Invalid task priority"
            );
        }

        // Validate estimated hours
        if (task.getEstimatedHours() != null &&
                task.getEstimatedHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Estimated hours must be greater than zero"
            );
        }

        // Save task
        Task savedTask = taskRepository.saveTask(task);

        // Record task creation
        recordActivity(
                savedTask.getTaskId(),
                savedTask.getCreatedBy(),
                "CREATED",
                null,
                savedTask.getTitle(),
                "Task created"
        );

        // Notify assigned user
        if (savedTask.getAssignedTo() != null) {

            recordActivity(
                    savedTask.getTaskId(),
                    savedTask.getCreatedBy(),
                    "ASSIGNED",
                    null,
                    savedTask.getAssignedTo().toString(),
                    "Task assigned to user"
            );

            Notification notification = new Notification(
                    savedTask.getAssignedTo(),
                    savedTask.getTaskId(),
                    "New Task Assigned",
                    "You have been assigned a task: " +
                            savedTask.getTitle(),
                    "TASK_ASSIGNED"
            );

            notificationRepository.saveNotification(notification);
        }

        return savedTask;
    }

    // 2. Get task by ID
    public Task getTaskById(Long taskId) {

        Task task = taskRepository.getTaskById(taskId);

        if (task == null) {
            throw new IllegalArgumentException(
                    "Task not found"
            );
        }

        return task;
    }

    // 3. Get all tasks
    public List<Task> getAllTasks() {

        return taskRepository.getAllTasks();
    }

    // 4. Get tasks assigned to a user
    public List<Task> getTasksAssignedToUser(Long userId) {

        validateActiveUser(userId);

        return taskRepository.getTasksAssignedToUser(userId);
    }

    // 5. Get tasks created by an admin
    public List<Task> getTasksCreatedBy(Long adminId) {

        validateActiveUser(adminId);

        return taskRepository.getTasksCreatedBy(adminId);
    }

    // 6. Update task details
    public Task updateTask(Task updatedTask) {

        Task existingTask = getTaskById(
                updatedTask.getTaskId()
        );

        if (updatedTask.getTitle() == null ||
                updatedTask.getTitle().isBlank()) {
            throw new IllegalArgumentException(
                    "Task title cannot be empty"
            );
        }

        if (updatedTask.getAssignedTo() != null) {
            validateActiveUser(updatedTask.getAssignedTo());
        }

        if (!isValidStatus(updatedTask.getStatus()) ||
                !isValidPriority(updatedTask.getPriority())) {
            throw new IllegalArgumentException(
                    "Invalid task status or priority"
            );
        }

        // Save updated task
        boolean updated = taskRepository.updateTask(updatedTask);

        if (!updated) {
            throw new IllegalArgumentException(
                    "Unable to update task"
            );
        }

        // Record assignment change
        if (existingTask.getAssignedTo() == null
                ? updatedTask.getAssignedTo() != null
                : !existingTask.getAssignedTo().equals(
                        updatedTask.getAssignedTo())) {

            recordActivity(
                    updatedTask.getTaskId(),
                    existingTask.getCreatedBy(),
                    "ASSIGNED",
                    existingTask.getAssignedTo() == null
                            ? null : existingTask.getAssignedTo().toString(),
                    updatedTask.getAssignedTo() == null
                            ? null : updatedTask.getAssignedTo().toString(),
                    "Task assignment updated"
            );

            if (updatedTask.getAssignedTo() != null) {

                Notification notification = new Notification(
                        updatedTask.getAssignedTo(),
                        updatedTask.getTaskId(),
                        "Task Assigned",
                        "A task has been assigned to you: " +
                                updatedTask.getTitle(),
                        "TASK_ASSIGNED"
                );

                notificationRepository.saveNotification(notification);
            }
        }

        return updatedTask;
    }

    // 7. Update task status
    public boolean updateTaskStatus(
            Long taskId, Long performedBy, String newStatus) {

        Task task = getTaskById(taskId);

        validateActiveUser(performedBy);

        if (!isValidStatus(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid task status"
            );
        }

        String oldStatus = task.getStatus();

        if (oldStatus.equals(newStatus)) {
            return false;
        }

        boolean updated = taskRepository.updateTaskStatus(
                taskId, newStatus
        );

        if (updated) {

            recordActivity(
                    taskId,
                    performedBy,
                    "STATUS_CHANGED",
                    oldStatus,
                    newStatus,
                    "Task status updated"
            );

            if (newStatus.equals("COMPLETED") &&
                    task.getAssignedTo() != null) {

                Notification notification = new Notification(
                        task.getAssignedTo(),
                        taskId,
                        "Task Completed",
                        "Task completed: " + task.getTitle(),
                        "TASK_COMPLETED"
                );

                notificationRepository.saveNotification(notification);
            }
        }

        return updated;
    }

    // 8. Update task progress
    public boolean updateTaskProgress(
            Long taskId, Long performedBy, Integer progress) {

        Task task = getTaskById(taskId);

        validateActiveUser(performedBy);

        if (progress == null || progress < 0 || progress > 100) {
            throw new IllegalArgumentException(
                    "Progress must be between 0 and 100"
            );
        }

        Integer oldProgress = task.getProgressPercent();

        boolean updated = taskRepository.updateTaskProgress(
                taskId, progress
        );

        if (updated) {

            recordActivity(
                    taskId,
                    performedBy,
                    "PROGRESS_UPDATED",
                    String.valueOf(oldProgress),
                    String.valueOf(progress),
                    "Task progress updated"
            );
        }

        return updated;
    }

    // 9. Cancel a task
    public boolean cancelTask(Long taskId, Long performedBy) {

        return updateTaskStatus(
                taskId, performedBy, "CANCELLED"
        );
    }
}
