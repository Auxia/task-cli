package com.taskmanager.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Objects;

public class Task {
    private final int id;
    private String description;
    private TaskStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Task(int id, String description) {
        this.id = validateId(id);
        this.description = validateDescription(description);
        this.status = TaskStatus.TODO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @JsonCreator
    public Task(@JsonProperty("id") int id,
                @JsonProperty("description") String description,
                @JsonProperty("status") TaskStatus status,
                @JsonProperty("createdAt") LocalDateTime createdAt,
                @JsonProperty("updatedAt") LocalDateTime updatedAt) {
        this.id = validateId(id);
        this.description = validateDescription(description);
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated date cannot be null");
    }

    private int validateId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Task ID must be positive");
        }
        return id;
    }

    private String validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Task description cannot be null or empty");
        }
        return description.trim();
    }

    public int getId() { return id; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void updateDescription(String newDescription) {
        this.description = validateDescription(newDescription);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(TaskStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "Status cannot be null");
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Task{id=%d, description='%s', status=%s, created=%s, updated=%s}",
                id, description, status, createdAt, updatedAt);
    }
}
