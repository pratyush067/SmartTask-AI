package com.smarttask.controller;

import com.smarttask.model.Notification;
import com.smarttask.service.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController
{
    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    // Create a notification
    @PostMapping
    public ResponseEntity<Notification> createNotification(
            @RequestBody Notification notification) {

        Notification saved =
            notificationService.createNotification(notification);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Get all notifications of a user
    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
            notificationService.getNotificationsByUserId(userId)
        );
    }

    // Get unread notifications
    @GetMapping("/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
            notificationService.getUnreadNotifications(userId)
        );
    }

    // Count unread notifications
    @GetMapping("/{userId}/unread/count")
    public ResponseEntity<Integer> countUnreadNotifications(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
            notificationService.countUnreadNotifications(userId)
        );
    }

    // Mark one notification as read
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long notificationId) {

        notificationService.markAsRead(notificationId);

        return ResponseEntity.ok(
            "Notification marked as read."
        );
    }

    // Mark all notifications as read
    @PutMapping("/{userId}/read-all")
    public ResponseEntity<String> markAllAsRead(
            @PathVariable Long userId) {

        notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(
            "All notifications marked as read."
        );
    }

    // Delete a notification
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String> deleteNotification(
            @PathVariable Long notificationId) {

        notificationService.deleteNotification(notificationId);

        return ResponseEntity.ok(
            "Notification deleted successfully."
        );
    }
}