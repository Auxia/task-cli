package com.taskmanager.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

class TaskTest {

    @Test
    @DisplayName("Should create task with valid parameters")
    void shouldCreateTaskWithValidParameters() {
        Task task = new Task(1, "Test task");

        assertEquals(1, task.getId());
        assertEquals("Test task", task.getDescription());
        assertEquals(TaskStatus.TODO, task.getStatus());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    @DisplayName("Should throw exception for invalid ID")
    void shouldThrowExceptionForInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> new Task(0, "Test"));
        assertThrows(IllegalArgumentException.class, () -> new Task(-1, "Test"));
    }

    @Test
    @DisplayName("Should throw exception for null or empty description")
    void shouldThrowExceptionForInvalidDescription() {
        assertThrows(IllegalArgumentException.class, () -> new Task(1, null));
        assertThrows(IllegalArgumentException.class, () -> new Task(1, ""));
        assertThrows(IllegalArgumentException.class, () -> new Task(1, "   "));
    }

    @Test
    @DisplayName("Should trim description whitespace")
    void shouldTrimDescriptionWhitespace() {
        Task task = new Task(1, "  Test task  ");
        assertEquals("Test task", task.getDescription());
    }

    @Test
    @DisplayName("Should update description and timestamp")
    void shouldUpdateDescriptionAndTimestamp() {
        Task task = new Task(1, "Original description");
        LocalDateTime originalUpdatedAt = task.getUpdatedAt();

        // Small delay to ensure timestamp difference
        try { Thread.sleep(1); } catch (InterruptedException e) {}

        task.updateDescription("New description");

        assertEquals("New description", task.getDescription());
        assertTrue(task.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    @DisplayName("Should update status and timestamp")
    void shouldUpdateStatusAndTimestamp() {
        Task task = new Task(1, "Test task");
        LocalDateTime originalUpdatedAt = task.getUpdatedAt();

        try { Thread.sleep(1); } catch (InterruptedException e) {}

        task.updateStatus(TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertTrue(task.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    @DisplayName("Should throw exception when updating with null status")
    void shouldThrowExceptionForNullStatus() {
        Task task = new Task(1, "Test task");
        assertThrows(NullPointerException.class, () -> task.updateStatus(null));
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        Task task1 = new Task(1, "Description 1");
        Task task2 = new Task(1, "Description 2");
        Task task3 = new Task(2, "Description 1");

        assertEquals(task1, task2); // Same ID
        assertNotEquals(task1, task3); // Different ID
        assertEquals(task1.hashCode(), task2.hashCode());
    }

    @Test
    @DisplayName("Should create task from JSON constructor")
    void shouldCreateTaskFromJsonConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Task task = new Task(1, "Test", TaskStatus.IN_PROGRESS, now, now);

        assertEquals(1, task.getId());
        assertEquals("Test", task.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(now, task.getCreatedAt());
        assertEquals(now, task.getUpdatedAt());
    }
}
