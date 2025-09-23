package com.taskmanager.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class TaskStatusTest {

    @Test
    @DisplayName("Should return correct display names")
    void shouldReturnCorrectDisplayNames() {
        assertEquals("todo", TaskStatus.TODO.getDisplayName());
        assertEquals("in-progress", TaskStatus.IN_PROGRESS.getDisplayName());
        assertEquals("done", TaskStatus.DONE.getDisplayName());
    }

    @Test
    @DisplayName("Should return correct JSON values")
    void shouldReturnCorrectJsonValues() {
        assertEquals("TODO", TaskStatus.TODO.getJsonValue());
        assertEquals("IN_PROGRESS", TaskStatus.IN_PROGRESS.getJsonValue());
        assertEquals("DONE", TaskStatus.DONE.getJsonValue());
    }

    @Test
    @DisplayName("Should parse from JSON value correctly")
    void shouldParseFromJsonValueCorrectly() {
        assertEquals(TaskStatus.TODO, TaskStatus.fromJsonValue("TODO"));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.fromJsonValue("IN_PROGRESS"));
        assertEquals(TaskStatus.DONE, TaskStatus.fromJsonValue("DONE"));
    }

    @Test
    @DisplayName("Should throw exception for invalid JSON value")
    void shouldThrowExceptionForInvalidJsonValue() {
        assertThrows(IllegalArgumentException.class,
                () -> TaskStatus.fromJsonValue("INVALID"));
    }

    @Test
    @DisplayName("Should parse from display name correctly")
    void shouldParseFromDisplayNameCorrectly() {
        assertEquals(TaskStatus.TODO, TaskStatus.fromDisplayName("todo"));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.fromDisplayName("in-progress"));
        assertEquals(TaskStatus.DONE, TaskStatus.fromDisplayName("done"));

        // Case insensitive
        assertEquals(TaskStatus.TODO, TaskStatus.fromDisplayName("TODO"));
        assertEquals(TaskStatus.DONE, TaskStatus.fromDisplayName("DONE"));
    }

    @Test
    @DisplayName("Should throw exception for invalid display name")
    void shouldThrowExceptionForInvalidDisplayName() {
        assertThrows(IllegalArgumentException.class,
                () -> TaskStatus.fromDisplayName("invalid"));
    }
}
