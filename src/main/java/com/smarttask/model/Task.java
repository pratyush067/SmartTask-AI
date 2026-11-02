package com.smarttask.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Task 
{
    private Long taskId;
    private String title;
    private String description;
    private Long createdBy;
    private Long assignedTo;
    private String status;
    private String priority;
    private LocalDateTime deadline;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private Integer progressPercent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Task() {

    }

    public Task(String title, String description,
                Long createdBy, Long assignedTo,
                String status, String priority,
                LocalDateTime deadline,
                BigDecimal estimatedHours) {

        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.assignedTo = assignedTo;
        this.status = status;
        this.priority = priority;
        this.deadline = deadline;
        this.estimatedHours = estimatedHours;
        this.progressPercent = 0;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(BigDecimal estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public BigDecimal getActualHours() {
        return actualHours;
    }

    public void setActualHours(BigDecimal actualHours) {
        this.actualHours = actualHours;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
