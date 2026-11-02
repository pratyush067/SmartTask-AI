package com.smarttask.service;

import com.smarttask.model.Notification;
import com.smarttask.model.User;
import com.smarttask.repository.NotificationRepository;
import com.smarttask.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService
{
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {

        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // Validate whether a user exists and is active
    private void validateActiveUser(Long userId) {

        User user = userRepository.getUserById(userId);

        if (user == null || !user.isActive()) {
            throw new RuntimeException(
                    "User does not exist or is inactive.");
        }
    }

    // Create a notification
    public Notification createNotification(Notification notification) {

        if (notification == null) {
            throw new IllegalArgumentException(
                    "Notification cannot be null.");
        }

        validateActiveUser(notification.getUserId());

        if (notification.getTitle() == null ||
                notification.getTitle().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Notification title is required.");
        }

        if (notification.getMessage() == null ||
                notification.getMessage().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Notification message is required.");
        }

        if (notification.getNotificationType() == null ||
                notification.getNotificationType().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Notification type is required.");
        }

        notificationRepository.saveNotification(notification);

        return notification;
    }

    // Get all notifications for a user
    public List<Notification> getNotificationsByUserId(Long userId) {

        validateActiveUser(userId);

        return notificationRepository.getNotificationsByUserId(userId);
    }

    // Get unread notifications
    public List<Notification> getUnreadNotifications(Long userId) {

        validateActiveUser(userId);

        return notificationRepository.getUnreadNotifications(userId);
    }

    // Mark an individual notification as read
    public void markAsRead(Long notificationId) {

        if (notificationId == null || notificationId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid notification ID.");
        }

        notificationRepository.markAsRead(notificationId);
    }

    // Mark all notifications of a user as read
    public void markAllAsRead(Long userId) {

        validateActiveUser(userId);

        notificationRepository.markAllAsRead(userId);
    }

    // Count unread notifications
    public int countUnreadNotifications(Long userId) {

        validateActiveUser(userId);

        return notificationRepository.countUnreadNotifications(userId);
    }

    // Delete a notification
    public void deleteNotification(Long notificationId) {

        if (notificationId == null || notificationId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid notification ID.");
        }

        notificationRepository.deleteNotification(notificationId);
    }
}
