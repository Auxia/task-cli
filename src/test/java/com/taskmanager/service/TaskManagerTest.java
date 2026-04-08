package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.repository.InMemoryTaskRepository;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

/**
 * Unit tests for TaskManager business logic.
 * Uses InMemoryTaskRepository — no disk I/O, no @TempDir.
 * File I/O edge cases live in JsonFileTaskRepositoryTest.
 */
class TaskManagerTest {

    private TaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = new TaskManager(new InMemoryTaskRepository());
    }

    @Test
    @DisplayName("Should add task successfully")
    void shouldAddTaskSuccessfully() {
        Task task = taskManager.addTask("Test task");

        assertNotNull(task);
        assertEquals(1, task.id());
        assertEquals("Test task", task.description());
        assertEquals(TaskStatus.TODO, task.status());
        assertTrue(taskManager.taskExists(1));
    }

    @Test
    @DisplayName("Should increment task IDs")
    void shouldIncrementTaskIds() {
        Task task1 = taskManager.addTask("First task");
        Task task2 = taskManager.addTask("Second task");

        assertEquals(1, task1.id());
        assertEquals(2, task2.id());
    }

    @Test
    @DisplayName("Should update existing task")
    void shouldUpdateExistingTask() {
        Task original = taskManager.addTask("Original description");
        Task updated = taskManager.updateTask(original.id(), "Updated description");

        assertEquals(original.id(), updated.id());
        assertEquals("Updated description", updated.description());
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent task")
    void shouldThrowExceptionWhenUpdatingNonExistentTask() {
        assertThrows(IllegalArgumentException.class,
                () -> taskManager.updateTask(999, "New description"));
    }

    @Test
    @DisplayName("Should remove task successfully")
    void shouldRemoveTaskSuccessfully() {
        Task task = taskManager.addTask("Task to remove");

        Task removed = taskManager.removeTask(task.id());

        assertEquals(task, removed);
        assertFalse(taskManager.taskExists(task.id()));
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent task")
    void shouldThrowExceptionWhenRemovingNonExistentTask() {
        assertThrows(IllegalArgumentException.class,
                () -> taskManager.removeTask(999));
    }

    @Test
    @DisplayName("Should update task status")
    void shouldUpdateTaskStatus() {
        Task task = taskManager.addTask("Test task");

        Task updated = taskManager.updateTaskStatus(task.id(), TaskStatus.DONE);

        assertEquals(TaskStatus.DONE, updated.status());
    }

    @Test
    @DisplayName("Should get all tasks")
    void shouldGetAllTasks() {
        taskManager.addTask("Task 1");
        taskManager.addTask("Task 2");
        taskManager.addTask("Task 3");

        List<Task> all = taskManager.getAllTasks();

        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("Should filter tasks by status")
    void shouldFilterTasksByStatus() {
        Task task1 = taskManager.addTask("Todo task");
        Task task2 = taskManager.addTask("Done task");
        taskManager.updateTaskStatus(task2.id(), TaskStatus.DONE);

        List<Task> todoTasks = taskManager.getTasksByStatus(TaskStatus.TODO);
        List<Task> doneTasks = taskManager.getTasksByStatus(TaskStatus.DONE);

        assertEquals(1, todoTasks.size());
        assertEquals(task1.id(), todoTasks.get(0).id());
        assertEquals(1, doneTasks.size());
        assertEquals(task2.id(), doneTasks.get(0).id());
    }

    @Test
    @DisplayName("Should handle empty task list")
    void shouldHandleEmptyTaskList() {
        assertEquals(0, taskManager.getTaskCount());
        assertTrue(taskManager.getAllTasks().isEmpty());
    }

    @Test
    @DisplayName("Should get task count correctly")
    void shouldGetTaskCountCorrectly() {
        assertEquals(0, taskManager.getTaskCount());

        taskManager.addTask("Task 1");
        assertEquals(1, taskManager.getTaskCount());

        Task task2 = taskManager.addTask("Task 2");
        assertEquals(2, taskManager.getTaskCount());

        taskManager.removeTask(task2.id());
        assertEquals(1, taskManager.getTaskCount());
    }

    @Test
    @DisplayName("Should get task by ID")
    void shouldGetTaskById() {
        taskManager.addTask("Target task");

        Task found = taskManager.getTaskById(1);

        assertEquals("Target task", found.description());
    }

    @Test
    @DisplayName("Should throw when getting non-existent task by ID")
    void shouldThrowWhenGettingNonExistentTask() {
        assertThrows(IllegalArgumentException.class, () -> taskManager.getTaskById(999));
    }

    @Test
    @DisplayName("Whitespace-only description is rejected")
    void whitespaceOnlyDescriptionRejected() {
        assertThrows(IllegalArgumentException.class, () -> taskManager.addTask("   "));
    }

    @Test
    @DisplayName("Description exceeding MAX_DESCRIPTION_LENGTH is rejected")
    void descriptionOverMaxLengthRejected() {
        String tooLong = "x".repeat(Task.MAX_DESCRIPTION_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> taskManager.addTask(tooLong));
    }

    @Test
    @DisplayName("Description at exactly MAX_DESCRIPTION_LENGTH is accepted")
    void descriptionAtMaxLengthAccepted() {
        String atLimit = "x".repeat(Task.MAX_DESCRIPTION_LENGTH);
        assertDoesNotThrow(() -> taskManager.addTask(atLimit));
    }
}
