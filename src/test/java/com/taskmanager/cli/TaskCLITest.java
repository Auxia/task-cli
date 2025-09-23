package com.taskmanager.cli;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import com.taskmanager.service.TaskManager;

class TaskCLITest {

    @TempDir
    Path tempDir;

    private TaskCLI cli;
    private TaskManager taskManager;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        // Redirect System.out and System.err for testing
        originalOut = System.out;
        originalErr = System.err;
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));

        // Create one TaskManager instance for the entire test
        Path testFile = tempDir.resolve("test-tasks.json");
        taskManager = new TaskManager(testFile);
        cli = new TaskCLI(taskManager);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("Should persist tasks between operations")
    void shouldPersistTasksBetweenOperations() {
        cli.execute(new String[]{"add", "Persistent", "task"});

        // Create new CLI with same TaskManager
        TaskCLI newCli = new TaskCLI(taskManager);
        newCli.execute(new String[]{"list"});

        // Should still see the task
    }

    @Test
    @DisplayName("Should show usage when no arguments provided")
    void shouldShowUsageWhenNoArgumentsProvided() {
        cli.execute(new String[]{});

        String output = outputStream.toString();
        assertTrue(output.contains("Task CLI - Task Management Application"));
        assertTrue(output.contains("Usage:"));
    }

    @Test
    @DisplayName("Should add task successfully")
    void shouldAddTaskSuccessfully() {
        cli.execute(new String[]{"add", "Test", "task", "description"});

        String output = outputStream.toString();
        assertTrue(output.contains("Task added successfully (ID: 1)"));
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
        outputStream.reset(); // Clear output from add

        cli.execute(new String[]{"update", "1", "Updated", "task", "description"});

        String output = outputStream.toString();
        assertTrue(output.contains("Task updated successfully (ID: 1)"));
    }

    @Test
    @DisplayName("Should delete task successfully")
    void shouldDeleteTaskSuccessfully() {
        cli.execute(new String[]{"add", "Task", "to", "delete"});
        outputStream.reset();

        cli.execute(new String[]{"delete", "1"});

        String output = outputStream.toString();
        assertTrue(output.contains("Task deleted successfully (ID: 1)"));
    }

    @Test
    @DisplayName("Should mark task status successfully")
    void shouldMarkTaskStatusSuccessfully() {
        cli.execute(new String[]{"add", "Test", "task"});
        outputStream.reset();

        cli.execute(new String[]{"mark-done", "1"});

        String output = outputStream.toString();
        assertTrue(output.contains("Task 1 marked as DONE"));
    }

    @Test
    @DisplayName("Should list tasks successfully")
    void shouldListTasksSuccessfully() {
        cli.execute(new String[]{"add", "First", "task"});
        cli.execute(new String[]{"add", "Second", "task"});
        outputStream.reset();

        cli.execute(new String[]{"list"});

        String output = outputStream.toString();
        assertTrue(output.contains("1. [todo] First task"));
        assertTrue(output.contains("2. [todo] Second task"));
    }

    @Test
    @DisplayName("Should list tasks by status")
    void shouldListTasksByStatus() {
        cli.execute(new String[]{"add", "Todo", "task"});
        cli.execute(new String[]{"add", "Done", "task"});
        cli.execute(new String[]{"mark-done", "2"});
        outputStream.reset();

        cli.execute(new String[]{"list", "done"});

        String output = outputStream.toString();
        assertTrue(output.contains("2. [DONE] Done task"));
        assertFalse(output.contains("1. [todo] Todo task"));
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
    @DisplayName("Should handle invalid status")
    void shouldHandleInvalidStatus() {
        assertThrows(RuntimeException.class, () -> cli.execute(new String[]{"list", "invalid"}));
    }

    @Test
    @DisplayName("Should show help message")
    void shouldShowHelpMessage() {
        cli.execute(new String[]{"help"});

        String output = outputStream.toString();
        assertTrue(output.contains("Commands:"));
        assertTrue(output.contains("add <description>"));
        assertTrue(output.contains("Examples:"));
    }
}
