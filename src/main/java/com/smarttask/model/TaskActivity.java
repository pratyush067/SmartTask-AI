package com.smarttask.model;

import java.time.LocalDateTime;

public class TaskActivity 
{
    private Long activityId;
    private Long taskId;
    private Long performedBy;
    private String activityType;
    private String oldValue;
    private String newValue;
    private String remarks;
    private LocalDateTime createdAt;

    public TaskActivity() {

    }

    public TaskActivity(Long taskId, Long performedBy,
                        String activityType,
                        String oldValue, String newValue,
                        String remarks) {

        this.taskId = taskId;
        this.performedBy = performedBy;
        this.activityType = activityType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.remarks = remarks;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(Long performedBy) {
        this.performedBy = performedBy;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
