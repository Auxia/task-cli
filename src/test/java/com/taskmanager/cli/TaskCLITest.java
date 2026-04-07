package com.taskmanager.cli;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.taskmanager.service.TaskManager;

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
    private ByteArrayOutputStream capturedOut;
    private PrintStream originalOut;

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
        // Capture System.out so tests can assert on console messages
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));

        // Create one TaskManager instance for the entire test
        Path testFile = tempDir.resolve("test-tasks.json");
        taskManager = new TaskManager(testFile);
        cli = new TaskCLI(taskManager);

        // Attach test log appender to capture logger output
        rootLogger = (Logger) LoggerFactory.getLogger("com.taskmanager");
        appender = new TestLogAppender();
        appender.start();
        rootLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        if (rootLogger != null && appender != null) {
            rootLogger.detachAppender(appender);
            appender.stop();
        }
    }

    private String getLogOutput() {
        return String.join("\n", appender.getMessages());
    }

    private String getConsoleOutput() {
        return capturedOut.toString();
    }

    private void clearConsoleOutput() {
        capturedOut.reset();
    }

    @Test
    @DisplayName("Should show usage when no arguments provided")
    void shouldShowUsageWhenNoArgumentsProvided() {
        cli.execute(new String[]{});

        String output = getLogOutput();
        assertTrue(output.contains("Task CLI - Task Management Application"));
        assertTrue(output.contains("Usage:"));
    }

    @Test
    @DisplayName("Should add task successfully")
    void shouldAddTaskSuccessfully() {
        cli.execute(new String[]{"add", "Test", "task", "description"});

        String output = getLogOutput();
        assertTrue(output.contains("Added task id=1") || output.contains("Task added successfully"),
                "Expected success message but got: " + output);
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
        appender.clear();

        cli.execute(new String[]{"update", "1", "Updated", "task", "description"});

        String output = getLogOutput();
        assertTrue(output.contains("Updated task id=1") || output.contains("Task updated successfully"),
                "Expected update success message but got: " + output);
    }

    @Test
    @DisplayName("Should delete task successfully")
    void shouldDeleteTaskSuccessfully() {
        cli.execute(new String[]{"add", "Task", "to", "delete"});
        appender.clear();

        cli.execute(new String[]{"delete", "1"});

        String output = getLogOutput();
        assertTrue(output.contains("Deleted task id=1") || output.contains("Task deleted successfully"),
                "Expected delete success message but got: " + output);
    }

    @Test
    @DisplayName("Should mark task status successfully")
    void shouldMarkTaskStatusSuccessfully() {
        cli.execute(new String[]{"add", "Test", "task"});
        appender.clear();

        cli.execute(new String[]{"mark-done", "1"});

        String output = getLogOutput();

        boolean hasCorrectMessage = output.contains("marked as") || output.contains("status changed to") || output.contains("marked as DONE");

        assertTrue(hasCorrectMessage,
                "Expected status change message but got: '" + output + "'");
    }

    @Test
    @DisplayName("Should list tasks successfully")
    void shouldListTasksSuccessfully() {
        cli.execute(new String[]{"add", "First", "task"});
        cli.execute(new String[]{"add", "Second", "task"});
        appender.clear();

        cli.execute(new String[]{"list"});

        String output = getLogOutput();

        assertTrue(output.contains("First task") || output.contains("First task"),
                "Expected 'First task' in output but got: " + output);
        assertTrue(output.contains("Second task") || output.contains("Second task"),
                "Expected 'Second task' in output but got: " + output);
    }

    @Test
    @DisplayName("Should list tasks by status")
    void shouldListTasksByStatus() {
        cli.execute(new String[]{"add", "Todo", "task"});
        cli.execute(new String[]{"add", "Done", "task"});
        cli.execute(new String[]{"mark-done", "2"});
        appender.clear();

        cli.execute(new String[]{"list", "done"});

        String output = getLogOutput();

        boolean hasDoneTask = output.contains("Done task") && output.contains("DONE");

        boolean excludesTodoTask = !output.contains("Todo task");

        assertTrue(hasDoneTask,
                "Expected 'Done task' with done status but got: " + output);
        assertTrue(excludesTodoTask,
                "Should not contain 'Todo task' but got: " + output);
    }

    @Test
    @DisplayName("Should handle empty task list")
    void shouldHandleEmptyTaskList() {
        cli.execute(new String[]{"list"});

        assertTrue(getConsoleOutput().contains("No tasks found"),
                "Expected 'No tasks found' message on stdout but got: " + getConsoleOutput());
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
        appender.clear();
        cli.execute(new String[]{"help"});

        String output = getLogOutput();
        assertTrue(output.contains("Commands:"));
        assertTrue(output.contains("add <description>"));
        assertTrue(output.contains("Examples:"));
    }

    @Test
    @DisplayName("Should handle multi-word task descriptions")
    void shouldHandleMultiWordTaskDescriptions() {
        cli.execute(new String[]{"add", "This", "is", "a", "multi", "word", "task", "description"});
        appender.clear();

        cli.execute(new String[]{"list"});

        String output = getLogOutput();
        assertTrue(output.contains("This is a multi word task description"),
                "Expected full description in output but got: " + output);
    }

    @Test
    @DisplayName("Should handle update with multi-word description")
    void shouldHandleUpdateWithMultiWordDescription() {
        cli.execute(new String[]{"add", "Original"});
        appender.clear();

        cli.execute(new String[]{"update", "1", "This", "is", "the", "new", "description"});

        String output = getLogOutput();
        assertTrue(output.contains("Updated task id=1") || output.contains("Task updated successfully (ID: 1)"),
                "Expected update success message but got: " + output);
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
        // Add a task
        cli.execute(new String[]{"add", "Persistent", "task"});

        // Create new CLI with same TaskManager to simulate app restart
        TaskCLI newCli = new TaskCLI(taskManager);
        appender.clear();

        // List tasks with new CLI instance
        newCli.execute(new String[]{"list"});

        String output = getLogOutput();
        assertTrue(output.contains("Persistent task"),
                "Task should persist between CLI instances but got: " + output);
    }

    @Test
    @DisplayName("Should maintain task count correctly")
    void shouldMaintainTaskCountCorrectly() {
        // Initially no tasks
        cli.execute(new String[]{"list"});
        assertTrue(getConsoleOutput().contains("No tasks found"));

        // Add two tasks
        cli.execute(new String[]{"add", "Task", "1"});
        cli.execute(new String[]{"add", "Task", "2"});
        clearConsoleOutput();

        cli.execute(new String[]{"list"});
        String afterAdd = getConsoleOutput();
        assertTrue(afterAdd.contains("Task 1"));
        assertTrue(afterAdd.contains("Task 2"));

        // Delete one task
        cli.execute(new String[]{"delete", "1"});
        clearConsoleOutput();

        cli.execute(new String[]{"list"});
        String afterDelete = getConsoleOutput();
        assertFalse(afterDelete.contains("1. [todo] Task 1"));
        assertTrue(afterDelete.contains("Task 2"));
    }
}
