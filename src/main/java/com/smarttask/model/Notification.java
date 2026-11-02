package com.smarttask.model;

import java.time.LocalDateTime;

public class Notification 
{
    private Long notificationId;
    private Long userId;
    private Long taskId;
    private String title;
    private String message;
    private String notificationType;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public Notification() {

    }

    public Notification(Long userId, Long taskId,
                        String title, String message,
                        String notificationType) {

        this.userId = userId;
        this.taskId = taskId;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType;
        this.read = false;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
