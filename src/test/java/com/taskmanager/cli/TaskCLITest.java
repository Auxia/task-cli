package com.taskmanager.cli;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.taskmanager.service.TaskManager;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

class TaskCLITest {

    @TempDir
    Path tempDir;

    private TaskCLI cli;
    private TaskManager taskManager;
    private TestLogAppender appender;
    private Logger rootLogger;

    static class TestLogAppender extends AppenderBase<ILoggingEvent> {
        private final List<ILoggingEvent> events = new java.util.ArrayList<>();
        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }
        public List<String> getMessages() {
            return events.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
        }
        public void clear() { events.clear(); }
    }

    @BeforeEach
    void setUp() {
        // Create one TaskManager instance for the entire test
        Path testFile = tempDir.resolve("test-tasks.json");
        taskManager = new TaskManager(testFile);
        cli = new TaskCLI(taskManager);

        // Attach test log appender to capture logger output (attach to root via LoggerContext)
        ch.qos.logback.classic.LoggerContext lc = (ch.qos.logback.classic.LoggerContext) LoggerFactory.getILoggerFactory();
        rootLogger = lc.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        appender = new TestLogAppender();
        appender.setContext(lc);
        appender.start();
        rootLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        if (rootLogger != null && appender != null) {
            rootLogger.detachAppender(appender);
            appender.stop();
        }
    }

    private String getLogOutput() {
        return String.join("\n", appender.getMessages());
    }

    @Test
    @DisplayName("Should show usage when no arguments provided")
    void shouldShowUsageWhenNoArgumentsProvided() {
        assertDoesNotThrow(() -> cli.execute(new String[]{}));
    }

    @Test
    @DisplayName("Should add task successfully")
    void shouldAddTaskSuccessfully() {
        cli.execute(new String[]{"add", "Test", "task", "description"});

        // Verify state in TaskManager rather than log output
        assertTrue(taskManager.taskExists(1));
        assertEquals("Test task description", taskManager.getTaskById(1).description());
    }

    @Test
    @DisplayName("Should handle add command without description")
    void shouldHandleAddCommandWithoutDescription() {
        assertThrows(RuntimeException.class, () -> cli.execute(new String[]{"add"}));
    }

    @Test
    @DisplayName("Should update task successfully")
    void shouldUpdateTaskSuccessfully() {
        cli.execute(new String[]{"add", "Original", "task"});

        cli.execute(new String[]{"update", "1", "Updated", "task", "description"});

        assertEquals("Updated task description", taskManager.getTaskById(1).description());
    }

    @Test
    @DisplayName("Should delete task successfully")
    void shouldDeleteTaskSuccessfully() {
        cli.execute(new String[]{"add", "Task", "to", "delete"});

        cli.execute(new String[]{"delete", "1"});

        assertFalse(taskManager.taskExists(1));
    }

    @Test
    @DisplayName("Should mark task status successfully")
    void shouldMarkTaskStatusSuccessfully() {
        cli.execute(new String[]{"add", "Test", "task"});

        cli.execute(new String[]{"mark-done", "1"});

        assertEquals(TaskStatus.DONE, taskManager.getTaskById(1).status());
    }

    @Test
    @DisplayName("Should list tasks successfully")
    void shouldListTasksSuccessfully() {
        cli.execute(new String[]{"add", "First", "task"});
        cli.execute(new String[]{"add", "Second", "task"});

        List<Task> all = taskManager.getAllTasks();
        List<String> descs = all.stream().map(Task::description).collect(Collectors.toList());

        assertTrue(descs.contains("First task"));
        assertTrue(descs.contains("Second task"));
    }

    @Test
    @DisplayName("Should list tasks by status")
    void shouldListTasksByStatus() {
        cli.execute(new String[]{"add", "Todo", "task"});
        cli.execute(new String[]{"add", "Done", "task"});
        cli.execute(new String[]{"mark-done", "2"});

        List<Task> done = taskManager.getTasksByStatus(TaskStatus.DONE);
        assertEquals(1, done.size());
        assertEquals("Done task", done.get(0).description());

        List<Task> todo = taskManager.getTasksByStatus(TaskStatus.TODO);
        assertTrue(todo.stream().noneMatch(t -> t.description().equals("Done task")));
    }

    @Test
    @DisplayName("Should handle empty task list")
    void shouldHandleEmptyTaskList() {
        // No tasks have been added in setup
        assertEquals(0, taskManager.getTaskCount());
        assertDoesNotThrow(() -> cli.execute(new String[]{"list"}));
    }

    @Test
    @DisplayName("Should handle invalid command gracefully")
    void shouldHandleInvalidCommandGracefully() {
        assertThrows(RuntimeException.class, () -> cli.execute(new String[]{"invalid"}));
    }

    @Test
    @DisplayName("Should handle invalid task ID")
    void shouldHandleInvalidTaskId() {
        assertThrows(RuntimeException.class, () -> cli.execute(new String[]{"delete", "abc"}));
        assertThrows(RuntimeException.class, () -> cli.execute(new String[]{"delete", "0"}));
    }

    @Test
    @DisplayName("Should handle non-existent task ID")
    void shouldHandleNonExistentTaskId() {
        assertThrows(RuntimeException.class, () -> cli.execute(new String[]{"delete", "999"}));
    }

    @Test
    @DisplayName("Should handle invalid status")
    void shouldHandleInvalidStatus() {
        assertThrows(RuntimeException.class, () -> cli.execute(new String[]{"list", "invalid"}));
    }

    @Test
    @DisplayName("Should show help message")
    void shouldShowHelpMessage() {
        assertDoesNotThrow(() -> cli.execute(new String[]{"help"}));
    }

    @Test
    @DisplayName("Should handle multi-word task descriptions")
    void shouldHandleMultiWordTaskDescriptions() {
        cli.execute(new String[]{"add", "This", "is", "a", "multi", "word", "task", "description"});

        List<Task> all = taskManager.getAllTasks();
        assertTrue(all.stream().anyMatch(t -> t.description().equals("This is a multi word task description")));
    }

    @Test
    @DisplayName("Should handle update with multi-word description")
    void shouldHandleUpdateWithMultiWordDescription() {
        cli.execute(new String[]{"add", "Original"});

        cli.execute(new String[]{"update", "1", "This", "is", "the", "new", "description"});

        assertEquals("This is the new description", taskManager.getTaskById(1).description());
    }

    @Test
    @DisplayName("Should require description for add command")
    void shouldRequireDescriptionForAddCommand() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cli.execute(new String[]{"add"}));
        assertTrue(exception.getMessage().contains("Add command requires a task description"));
    }

    @Test
    @DisplayName("Should require task ID for update command")
    void shouldRequireTaskIdForUpdateCommand() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cli.execute(new String[]{"update"}));
        assertTrue(exception.getMessage().contains("Update command requires task ID and new description"));
    }

    @Test
    @DisplayName("Should require both ID and description for update")
    void shouldRequireBothIdAndDescriptionForUpdate() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cli.execute(new String[]{"update", "1"}));
        assertTrue(exception.getMessage().contains("Update command requires task ID and new description"));
    }

    @Test
    @DisplayName("Should persist tasks between operations")
    void shouldPersistTasksBetweenOperations() {
        // Add a task and save
        cli.execute(new String[]{"add", "Persistent", "task"});

        // Re-create manager reading same file used in setup
        TaskManager reload = new TaskManager(tempDir.resolve("test-tasks.json"));

        assertTrue(reload.taskExists(1));
        assertEquals("Persistent task", reload.getTaskById(1).description());
    }
    @Test
    @DisplayName("Should maintain task count correctly")
    void shouldMaintainTaskCountCorrectly() {
        // Initially no tasks
        assertEquals(0, taskManager.getTaskCount());

        // Add two tasks
        cli.execute(new String[]{"add", "Task", "1"});
        cli.execute(new String[]{"add", "Task", "2"});

        assertEquals(2, taskManager.getTaskCount());
        assertTrue(taskManager.taskExists(1));
        assertTrue(taskManager.taskExists(2));

        // Delete one task
        cli.execute(new String[]{"delete", "1"});

        assertEquals(1, taskManager.getTaskCount());
        assertFalse(taskManager.taskExists(1));
        assertTrue(taskManager.taskExists(2));
    }
}
