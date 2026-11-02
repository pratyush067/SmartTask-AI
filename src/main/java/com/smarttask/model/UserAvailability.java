package com.smarttask.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserAvailability 
{
    private Long availabilityId;
    private Long userId;
    private LocalDate availabilityDate;
    private BigDecimal availableHours;
    private boolean available;
    private String remarks;
    private LocalDateTime createdAt;

    public UserAvailability() {

    }

    public UserAvailability(Long userId,
                            LocalDate availabilityDate,
                            BigDecimal availableHours,
                            boolean available,
                            String remarks) {

        this.userId = userId;
        this.availabilityDate = availabilityDate;
        this.availableHours = availableHours;
        this.available = available;
        this.remarks = remarks;
    }

    public Long getAvailabilityId() {
        return availabilityId;
    }

    public void setAvailabilityId(Long availabilityId) {
        this.availabilityId = availabilityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getAvailabilityDate() {
        return availabilityDate;
    }

    public void setAvailabilityDate(LocalDate availabilityDate) {
        this.availabilityDate = availabilityDate;
    }

    public BigDecimal getAvailableHours() {
        return availableHours;
    }

    public void setAvailableHours(BigDecimal availableHours) {
        this.availableHours = availableHours;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
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
