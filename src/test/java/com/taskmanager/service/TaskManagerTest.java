package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        Task originalTask = taskManager.addTask("Original description");
        Task updatedTask = taskManager.updateTask(originalTask.id(), "Updated description");

        assertEquals(originalTask.id(), updatedTask.id());
        assertEquals("Updated description", updatedTask.description());
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

        Task removedTask = taskManager.removeTask(task.id());

        assertEquals(task, removedTask);
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

        Task updatedTask = taskManager.updateTaskStatus(task.id(), TaskStatus.DONE);

        assertEquals(TaskStatus.DONE, updatedTask.status());
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
        taskManager.updateTaskStatus(task2.id(), TaskStatus.DONE);

        List<Task> todoTasks = taskManager.getTasksByStatus(TaskStatus.TODO);
        List<Task> doneTasks = taskManager.getTasksByStatus(TaskStatus.DONE);

        assertEquals(1, todoTasks.size());
        assertEquals(task1.id(), todoTasks.get(0).id());
        assertEquals(1, doneTasks.size());
        assertEquals(task2.id(), doneTasks.get(0).id());
    }

    @Test
    @DisplayName("Should save and load tasks correctly")
    void shouldSaveAndLoadTasksCorrectly() {
        // Add tasks and save
        Task task1 = taskManager.addTask("First task");
        Task task2 = taskManager.addTask("Second task");
        taskManager.updateTaskStatus(task2.id(), TaskStatus.DONE);
        taskManager.saveTasks();

        // Create new manager with same file
        TaskManager newManager = new TaskManager(testFile);

        // Verify tasks are loaded
        assertEquals(2, newManager.getTaskCount());
        assertTrue(newManager.taskExists(task1.id()));
        assertTrue(newManager.taskExists(task2.id()));
        assertEquals(TaskStatus.DONE, newManager.getTaskById(task2.id()).status());
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

        taskManager.removeTask(task2.id());
        assertEquals(1, taskManager.getTaskCount());
    }

    // Security: path traversal (issue #5)

    @Test
    @DisplayName("Should reject relative path that traverses above working directory")
    void shouldRejectRelativePathTraversal() {
        assertThrows(IllegalArgumentException.class,
                () -> new TaskManager(Path.of("../../etc/passwd")));
    }

    @Test
    @DisplayName("Should reject relative path with traversal embedded mid-path")
    void shouldRejectEmbeddedTraversal() {
        assertThrows(IllegalArgumentException.class,
                () -> new TaskManager(Path.of("data/../../etc/shadow")));
    }

    @Test
    @DisplayName("Should accept absolute path")
    void shouldAcceptAbsolutePath(@TempDir Path dir) {
        Path absolute = dir.resolve("tasks.json");
        assertDoesNotThrow(() -> new TaskManager(absolute));
    }

    @Test
    @DisplayName("Should accept relative path within working directory")
    void shouldAcceptRelativePathInWorkingDirectory() {
        // A plain filename stays within the working directory
        assertDoesNotThrow(() -> TaskManager.validatePath(Path.of("tasks.json")));
    }

    @Test
    @DisplayName("Should reject path traversal via tasks.file system property")
    void shouldRejectPathTraversalViaSystemProperty() {
        System.setProperty("tasks.file", "../../etc/passwd");
        try {
            assertThrows(IllegalArgumentException.class, TaskManager::new);
        } finally {
            System.clearProperty("tasks.file");
        }
    }

    // ---------- File I/O edge cases ----------

    @Test
    @DisplayName("Corrupted JSON throws descriptive RuntimeException on load")
    void corruptedJsonThrowsOnLoad() throws IOException {
        Path path = tempDir.resolve("corrupted.json");
        Files.writeString(path, "{invalid json");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> new TaskManager(path));
        assertTrue(ex.getMessage().contains("Failed to load tasks"),
                "Expected 'Failed to load tasks' in message but got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Empty file written to disk loads as empty task list")
    void emptyFileOnDiskLoadsEmpty() throws IOException {
        Path path = tempDir.resolve("empty.json");
        Files.writeString(path, "");
        TaskManager manager = new TaskManager(path);
        assertEquals(0, manager.getTaskCount());
    }

    @Test
    @DisplayName("Missing parent directory: construction succeeds, saveTasks throws RuntimeException")
    void missingParentDirectoryFailsOnSave() {
        Path path = tempDir.resolve("nonexistent/tasks.json");
        TaskManager manager = assertDoesNotThrow(() -> new TaskManager(path));
        manager.addTask("A task");
        RuntimeException ex = assertThrows(RuntimeException.class, manager::saveTasks);
        assertTrue(ex.getMessage().contains("Failed to save tasks"),
                "Expected 'Failed to save tasks' in message but got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Read-only parent directory causes saveTasks to throw RuntimeException")
    void readOnlyDirectoryThrowsOnSave() throws IOException {
        assumeFalse("root".equals(System.getProperty("user.name")),
                "Skipped: setReadOnly() has no effect when running as root");
        // saveTasks() creates a temp file in the parent dir before moving — making
        // the directory read-only prevents temp file creation and triggers the error.
        Path roDir = Files.createDirectory(tempDir.resolve("ro"));
        Path path = roDir.resolve("tasks.json");
        TaskManager manager = new TaskManager(path);
        manager.addTask("A task");
        manager.saveTasks(); // first save succeeds while dir is writable
        roDir.toFile().setReadOnly();
        try {
            assertThrows(RuntimeException.class, manager::saveTasks);
        } finally {
            roDir.toFile().setWritable(true);
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("10,000 tasks load and save without OOM or timeout")
    void largeTaskFileHandledCorrectly() {
        for (int i = 0; i < 10_000; i++) {
            taskManager.addTask("Task " + i);
        }
        taskManager.saveTasks();

        TaskManager reloaded = new TaskManager(testFile);
        assertEquals(10_000, reloaded.getTaskCount());
        assertEquals(10_000, reloaded.getTaskById(10_000).id());
    }

    @Test
    @DisplayName("Whitespace-only description is rejected with IllegalArgumentException")
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
