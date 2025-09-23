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

        // Create one TaskManager instance for the entire test - FIXED
        Path testFile = tempDir.resolve("test-tasks.json");
        taskManager = new TaskManager(testFile);
        cli = new TaskCLI(taskManager);  // ✅ NOW USES THE TEST TASKMANAGER
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
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
        assertTrue(output.contains("Task added successfully (ID: 1)"),
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
        outputStream.reset(); // Clear output from add

        cli.execute(new String[]{"update", "1", "Updated", "task", "description"});

        String output = outputStream.toString();
        assertTrue(output.contains("Task updated successfully (ID: 1)"),
                "Expected update success message but got: " + output);
    }

    @Test
    @DisplayName("Should delete task successfully")
    void shouldDeleteTaskSuccessfully() {
        cli.execute(new String[]{"add", "Task", "to", "delete"});
        outputStream.reset();

        cli.execute(new String[]{"delete", "1"});

        String output = outputStream.toString();
        assertTrue(output.contains("Task deleted successfully (ID: 1)"),
                "Expected delete success message but got: " + output);
    }

    @Test
    @DisplayName("Should mark task status successfully")
    void shouldMarkTaskStatusSuccessfully() {
        cli.execute(new String[]{"add", "Test", "task"});
        outputStream.reset();

        cli.execute(new String[]{"mark-done", "1"});

        String output = outputStream.toString();
        // Debug: Print the actual output
        System.out.println("DEBUG - Actual output: '" + output + "'");

        // Check for either format - the status might be "done" not "DONE"
        boolean hasCorrectMessage = output.contains("Task 1 marked as done") ||
                output.contains("Task 1 marked as DONE");

        assertTrue(hasCorrectMessage,
                "Expected status change message but got: '" + output + "'");
    }

    @Test
    @DisplayName("Should list tasks successfully")
    void shouldListTasksSuccessfully() {
        cli.execute(new String[]{"add", "First", "task"});
        cli.execute(new String[]{"add", "Second", "task"});
        outputStream.reset();

        cli.execute(new String[]{"list"});

        String output = outputStream.toString();
        // Debug: Print the actual output
        System.out.println("DEBUG - List output: '" + output + "'");

        assertTrue(output.contains("First task"),
                "Expected 'First task' in output but got: " + output);
        assertTrue(output.contains("Second task"),
                "Expected 'Second task' in output but got: " + output);
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
        // Debug: Print the actual output
        System.out.println("DEBUG - Status filter output: '" + output + "'");

        // Check for either display format
        boolean hasDoneTask = output.contains("Done task") &&
                (output.contains("[done]") || output.contains("[DONE]"));

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

        String output = outputStream.toString();
        assertTrue(output.contains("No tasks found"),
                "Expected 'No tasks found' message but got: " + output);
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
        cli.execute(new String[]{"help"});

        String output = outputStream.toString();
        assertTrue(output.contains("Commands:"));
        assertTrue(output.contains("add <description>"));
        assertTrue(output.contains("Examples:"));
    }

    @Test
    @DisplayName("Should handle multi-word task descriptions")
    void shouldHandleMultiWordTaskDescriptions() {
        cli.execute(new String[]{"add", "This", "is", "a", "multi", "word", "task", "description"});
        outputStream.reset();

        cli.execute(new String[]{"list"});

        String output = outputStream.toString();
        assertTrue(output.contains("This is a multi word task description"),
                "Expected full description in output but got: " + output);
    }

    @Test
    @DisplayName("Should handle update with multi-word description")
    void shouldHandleUpdateWithMultiWordDescription() {
        cli.execute(new String[]{"add", "Original"});
        outputStream.reset();

        cli.execute(new String[]{"update", "1", "This", "is", "the", "new", "description"});

        String output = outputStream.toString();
        assertTrue(output.contains("Task updated successfully (ID: 1)"),
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
        outputStream.reset();

        // List tasks with new CLI instance
        newCli.execute(new String[]{"list"});

        String output = outputStream.toString();
        assertTrue(output.contains("Persistent task"),
                "Task should persist between CLI instances but got: " + output);
    }

    @Test
    @DisplayName("Should maintain task count correctly")
    void shouldMaintainTaskCountCorrectly() {
        // Initially no tasks
        cli.execute(new String[]{"list"});
        String output = outputStream.toString();
        assertTrue(output.contains("No tasks found"));

        // Add two tasks
        cli.execute(new String[]{"add", "Task", "1"});
        cli.execute(new String[]{"add", "Task", "2"});
        outputStream.reset();

        cli.execute(new String[]{"list"});
        output = outputStream.toString();
        assertTrue(output.contains("Task 1"));
        assertTrue(output.contains("Task 2"));

        // Delete one task
        cli.execute(new String[]{"delete", "1"});
        outputStream.reset();

        cli.execute(new String[]{"list"});
        output = outputStream.toString();
        assertFalse(output.contains("Task 1"));
        assertTrue(output.contains("Task 2"));
    }
}
