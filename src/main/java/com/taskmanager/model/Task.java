package com.taskmanager.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Objects;

// Immutable record-based Task (modernized). Provides convenience update methods that return new Task instances.
public record Task(
        int id,
        String description,
        TaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final int MAX_DESCRIPTION_LENGTH = 10_000;

    public Task {
        if (id <= 0) throw new IllegalArgumentException("Task ID must be positive");
        if (description == null) throw new IllegalArgumentException("Task description cannot be null or empty");
        description = description.trim();
        if (description.isEmpty()) throw new IllegalArgumentException("Task description cannot be null or empty");
        if (description.length() > MAX_DESCRIPTION_LENGTH)
            throw new IllegalArgumentException(
                "Task description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH + " characters");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(createdAt, "Created date cannot be null");
        Objects.requireNonNull(updatedAt, "Updated date cannot be null");
    }

    // Convenience constructor for creating a new task
    public Task(int id, String description) {
        // Delegate to canonical constructor which performs validation (including null/empty check)
        this(id, description, TaskStatus.TODO, LocalDateTime.now(), LocalDateTime.now());
    }


    public Task updateDescription(String newDescription) {
        String desc = (newDescription == null) ? null : newDescription.trim();
        if (desc == null || desc.isEmpty()) throw new IllegalArgumentException("Task description cannot be null or empty");
        return new Task(this.id, desc, this.status, this.createdAt, LocalDateTime.now());
    }

    public Task updateStatus(TaskStatus newStatus) {
        Objects.requireNonNull(newStatus, "Status cannot be null");
        return new Task(this.id, this.description, newStatus, this.createdAt, LocalDateTime.now());
    }

    /**
     * Equality is intentionally based on ID only. Two Task instances with the same ID
     * represent the same task entity regardless of their current field values — this mirrors
     * how tasks are stored and looked up (keyed by ID). It is NOT value equality.
     * Consequence: a Set<Task> enforces ID uniqueness, not field-level uniqueness.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task t)) return false;
        return this.id == t.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
