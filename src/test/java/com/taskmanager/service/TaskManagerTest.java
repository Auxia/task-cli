package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;

class TaskManagerTest {

    @TempDir
    Path tempDir;

    private TaskManager taskManager;
    private Path testFile;

    @BeforeEach
    void setUp() {
        testFile = tempDir.resolve("test-tasks.json");
        taskManager = new TaskManager(testFile);
    }

    @Test
    @DisplayName("Should add task successfully")
    void shouldAddTaskSuccessfully() {
        Task task = taskManager.addTask("Test task");

        assertNotNull(task);
        assertEquals(1, task.getId());
        assertEquals("Test task", task.getDescription());
        assertEquals(TaskStatus.TODO, task.getStatus());
        assertTrue(taskManager.taskExists(1));
    }

    @Test
    @DisplayName("Should increment task IDs")
    void shouldIncrementTaskIds() {
        Task task1 = taskManager.addTask("First task");
        Task task2 = taskManager.addTask("Second task");

        assertEquals(1, task1.getId());
        assertEquals(2, task2.getId());
    }

    @Test
    @DisplayName("Should update existing task")
    void shouldUpdateExistingTask() {
        Task originalTask = taskManager.addTask("Original description");
        Task updatedTask = taskManager.updateTask(originalTask.getId(), "Updated description");

        assertEquals(originalTask.getId(), updatedTask.getId());
        assertEquals("Updated description", updatedTask.getDescription());
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

        Task removedTask = taskManager.removeTask(task.getId());

        assertEquals(task, removedTask);
        assertFalse(taskManager.taskExists(task.getId()));
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

        Task updatedTask = taskManager.updateTaskStatus(task.getId(), TaskStatus.DONE);

        assertEquals(TaskStatus.DONE, updatedTask.getStatus());
    }

    @Test
    @DisplayName("Should get all tasks")
    void shouldGetAllTasks() {
        taskManager.addTask("Task 1");
        taskManager.addTask("Task 2");
        taskManager.addTask("Task 3");

        List<Task> allTasks = taskManager.getAllTasks();

        assertEquals(3, allTasks.size());
    }

    @Test
    @DisplayName("Should filter tasks by status")
    void shouldFilterTasksByStatus() {
        Task task1 = taskManager.addTask("Todo task");
        Task task2 = taskManager.addTask("Done task");
        taskManager.updateTaskStatus(task2.getId(), TaskStatus.DONE);

        List<Task> todoTasks = taskManager.getTasksByStatus(TaskStatus.TODO);
        List<Task> doneTasks = taskManager.getTasksByStatus(TaskStatus.DONE);

        assertEquals(1, todoTasks.size());
        assertEquals(task1.getId(), todoTasks.get(0).getId());
        assertEquals(1, doneTasks.size());
        assertEquals(task2.getId(), doneTasks.get(0).getId());
    }

    @Test
    @DisplayName("Should save and load tasks correctly")
    void shouldSaveAndLoadTasksCorrectly() {
        // Add tasks and save
        Task task1 = taskManager.addTask("First task");
        Task task2 = taskManager.addTask("Second task");
        taskManager.updateTaskStatus(task2.getId(), TaskStatus.DONE);
        taskManager.saveTasks();

        // Create new manager with same file
        TaskManager newManager = new TaskManager(testFile);

        // Verify tasks are loaded
        assertEquals(2, newManager.getTaskCount());
        assertTrue(newManager.taskExists(task1.getId()));
        assertTrue(newManager.taskExists(task2.getId()));
        assertEquals(TaskStatus.DONE, newManager.getTaskById(task2.getId()).getStatus());
    }

    @Test
    @DisplayName("Should handle empty file gracefully")
    void shouldHandleEmptyFileGracefully() {
        TaskManager emptyManager = new TaskManager(tempDir.resolve("empty.json"));

        assertEquals(0, emptyManager.getTaskCount());
        assertDoesNotThrow(() -> emptyManager.getAllTasks());
    }

    @Test
    @DisplayName("Should get task count correctly")
    void shouldGetTaskCountCorrectly() {
        assertEquals(0, taskManager.getTaskCount());

        taskManager.addTask("Task 1");
        assertEquals(1, taskManager.getTaskCount());

        Task task2 = taskManager.addTask("Task 2");
        assertEquals(2, taskManager.getTaskCount());

        taskManager.removeTask(task2.getId());
        assertEquals(1, taskManager.getTaskCount());
    }
}
