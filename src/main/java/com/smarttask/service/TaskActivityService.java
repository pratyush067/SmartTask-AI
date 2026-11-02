package com.smarttask.service;

import com.smarttask.model.TaskActivity;
import com.smarttask.model.Task;
import com.smarttask.model.User;

import com.smarttask.repository.TaskActivityRepository;
import com.smarttask.repository.TaskRepository;
import com.smarttask.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class TaskActivityService 
{
    private final TaskActivityRepository activityRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // Constructor Injection
    public TaskActivityService(
            TaskActivityRepository activityRepository,
            TaskRepository taskRepository,
            UserRepository userRepository) {

        this.activityRepository = activityRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    // Allowed activity types
    private static final Set<String> VALID_ACTIVITY_TYPES = Set.of(
            "CREATED",
            "ASSIGNED",
            "STATUS_CHANGED",
            "PRIORITY_CHANGED",
            "DEADLINE_CHANGED",
            "PROGRESS_UPDATED",
            "COMMENT_ADDED",
            "DEPENDENCY_ADDED",
            "DEPENDENCY_REMOVED");

    // Validate task
    private void validateTask(Long taskId) {

        Task task = taskRepository.getTaskById(taskId);

        if (task == null) {
            throw new IllegalArgumentException(
                    "Task not found");
        }
    }

    // Validate user
    private void validateUser(Long userId) {

        User user = userRepository.getUserById(userId);

        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException(
                    "User does not exist or is inactive");
        }
    }

    // 1. Record a new activity
    public TaskActivity recordActivity(TaskActivity activity) {

        validateTask(activity.getTaskId());
        validateUser(activity.getPerformedBy());

        if (activity.getActivityType() == null ||
                !VALID_ACTIVITY_TYPES.contains(
                        activity.getActivityType())) {

            throw new IllegalArgumentException(
                    "Invalid activity type");
        }

        return activityRepository.saveActivity(activity);
    }

    // 2. Get activity history of a task
    public List<TaskActivity> getActivitiesByTaskId(Long taskId) {

        validateTask(taskId);

        return activityRepository.getActivitiesByTaskId(taskId);
    }

    // 3. Get activities performed by a user
    public List<TaskActivity> getActivitiesByUserId(Long userId) {

        validateUser(userId);

        return activityRepository.getActivitiesByUserId(userId);
    }

    // 4. Get all activities
    public List<TaskActivity> getAllActivities() {

        return activityRepository.getAllActivities();
    }
}