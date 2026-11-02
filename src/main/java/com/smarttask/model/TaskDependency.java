package com.smarttask.model;

import java.time.LocalDateTime;

public class TaskDependency 
{
    private Long dependencyId;
    private Long taskId;
    private Long dependsOnTaskId;
    private LocalDateTime createdAt;

    public TaskDependency() {

    }

    public TaskDependency(Long taskId, Long dependsOnTaskId) {
        this.taskId = taskId;
        this.dependsOnTaskId = dependsOnTaskId;
    }

    public Long getDependencyId() {
        return dependencyId;
    }

    public void setDependencyId(Long dependencyId) {
        this.dependencyId = dependencyId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getDependsOnTaskId() {
        return dependsOnTaskId;
    }

    public void setDependsOnTaskId(Long dependsOnTaskId) {
        this.dependsOnTaskId = dependsOnTaskId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}