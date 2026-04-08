package com.taskmanager.cli;

import com.taskmanager.model.TaskStatus;
import com.taskmanager.repository.JsonFileTaskRepository;
import com.taskmanager.service.TaskManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TaskCLITest {

    @TempDir
    Path tempDir;

    private TaskManager taskManager;
    private CommandLine cli;
    private ByteArrayOutputStream capturedOut;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));

        taskManager = new TaskManager(new JsonFileTaskRepository(tempDir.resolve("test-tasks.json")));
        cli = TaskCLI.buildCommandLine(taskManager);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private int run(String... args) {
        return cli.execute(args);
    }

    private String console() {
        return capturedOut.toString();
    }

    private void clearConsole() {
        capturedOut.reset();
    }

    // ---------- add ----------

    @Test
    @DisplayName("add: creates a task and prints confirmation")
    void addCreatesTask() {
        assertEquals(0, run("add", "Buy groceries"));
        assertTrue(console().contains("Task added successfully (ID: 1)"));
        assertEquals(1, taskManager.getTaskCount());
        assertEquals("Buy groceries", taskManager.getTaskById(1).description());
    }

    @Test
    @DisplayName("add: joins multi-word description into a single string")
    void addJoinsMultiWordDescription() {
        assertEquals(0, run("add", "Buy", "milk", "and", "eggs"));
        assertEquals("Buy milk and eggs", taskManager.getTaskById(1).description());
    }

    @Test
    @DisplayName("add: missing description returns non-zero exit code")
    void addWithNoDescriptionFails() {
        assertNotEquals(0, run("add"));
    }

    // ---------- update ----------

    @Test
    @DisplayName("update: changes task description and prints confirmation")
    void updateChangesDescription() {
        run("add", "Original");
        clearConsole();
        assertEquals(0, run("update", "1", "Updated description"));
        assertTrue(console().contains("Task updated successfully (ID: 1)"));
        assertEquals("Updated description", taskManager.getTaskById(1).description());
    }

    @Test
    @DisplayName("update: joins multi-word new description")
    void updateJoinsMultiWordDescription() {
        run("add", "Original");
        run("update", "1", "New", "multi", "word", "description");
        assertEquals("New multi word description", taskManager.getTaskById(1).description());
    }

    @Test
    @DisplayName("update: non-existent ID returns non-zero exit code")
    void updateNonExistentIdFails() {
        assertNotEquals(0, run("update", "999", "desc"));
    }

    @Test
    @DisplayName("update: missing arguments returns non-zero exit code")
    void updateMissingArgsFails() {
        assertNotEquals(0, run("update"));
        assertNotEquals(0, run("update", "1"));
    }

    // ---------- delete ----------

    @Test
    @DisplayName("delete: removes task and prints confirmation")
    void deleteRemovesTask() {
        run("add", "To delete");
        clearConsole();
        assertEquals(0, run("delete", "1"));
        assertTrue(console().contains("Task deleted successfully (ID: 1)"));
        assertEquals(0, taskManager.getTaskCount());
    }

    @Test
    @DisplayName("delete: non-existent ID returns non-zero exit code")
    void deleteNonExistentIdFails() {
        assertNotEquals(0, run("delete", "999"));
    }

    @Test
    @DisplayName("delete: non-numeric ID returns non-zero exit code")
    void deleteNonNumericIdFails() {
        assertNotEquals(0, run("delete", "abc"));
    }

    // ---------- mark commands ----------

    @Test
    @DisplayName("mark-done: updates status and prints confirmation")
    void markDoneUpdatesStatus() {
        run("add", "Task");
        clearConsole();
        assertEquals(0, run("mark-done", "1"));
        assertTrue(console().contains("Task 1 marked as done"));
        assertEquals(TaskStatus.DONE, taskManager.getTaskById(1).status());
    }

    @Test
    @DisplayName("mark-in-progress: updates status correctly")
    void markInProgressUpdatesStatus() {
        run("add", "Task");
        assertEquals(0, run("mark-in-progress", "1"));
        assertEquals(TaskStatus.IN_PROGRESS, taskManager.getTaskById(1).status());
    }

    @Test
    @DisplayName("mark-todo: updates status correctly")
    void markTodoUpdatesStatus() {
        run("add", "Task");
        run("mark-done", "1");
        assertEquals(0, run("mark-todo", "1"));
        assertEquals(TaskStatus.TODO, taskManager.getTaskById(1).status());
    }

    @Test
    @DisplayName("mark-done: non-existent ID returns non-zero exit code")
    void markDoneNonExistentIdFails() {
        assertNotEquals(0, run("mark-done", "999"));
    }

    // ---------- list ----------

    @Test
    @DisplayName("list: prints all tasks sorted by ID")
    void listPrintsAllTasks() {
        run("add", "First");
        run("add", "Second");
        clearConsole();
        assertEquals(0, run("list"));
        String out = console();
        assertTrue(out.contains("1. [todo] First"));
        assertTrue(out.contains("2. [todo] Second"));
    }

    @Test
    @DisplayName("list: prints 'No tasks found.' when empty")
    void listEmptyShowsMessage() {
        assertEquals(0, run("list"));
        assertTrue(console().contains("No tasks found."));
    }

    @Test
    @DisplayName("list: filters by status")
    void listFiltersByStatus() {
        run("add", "Todo task");
        run("add", "Done task");
        run("mark-done", "2");
        clearConsole();

        assertEquals(0, run("list", "done"));
        String out = console();
        assertTrue(out.contains("Done task"));
        assertFalse(out.contains("Todo task"));
    }

    @Test
    @DisplayName("list: invalid status returns non-zero exit code")
    void listInvalidStatusFails() {
        assertNotEquals(0, run("list", "invalid-status"));
    }

    // ---------- persistence ----------

    @Test
    @DisplayName("tasks persist to disk and are visible to a fresh TaskManager reloading the same file")
    void tasksPersistToDisk() {
        Path tasksFile = tempDir.resolve("test-tasks.json");
        run("add", "Persistent task");

        // Fresh TaskManager reading from the same file — simulates a real app restart
        TaskManager reloaded = new TaskManager(new JsonFileTaskRepository(tasksFile));
        assertEquals(1, reloaded.getTaskCount());
        assertEquals("Persistent task", reloaded.getTaskById(1).description());
        assertEquals(TaskStatus.TODO, reloaded.getTaskById(1).status());
    }

    @Test
    @DisplayName("task count is correct across add and delete operations")
    void taskCountIsCorrectAcrossOperations() {
        assertEquals(0, run("list")); // empty
        assertTrue(console().contains("No tasks found."));

        run("add", "Task 1");
        run("add", "Task 2");
        clearConsole();

        run("list");
        String afterAdd = console();
        assertTrue(afterAdd.contains("Task 1"));
        assertTrue(afterAdd.contains("Task 2"));

        run("delete", "1");
        clearConsole();

        run("list");
        String afterDelete = console();
        assertFalse(afterDelete.contains("1. [todo] Task 1"));
        assertTrue(afterDelete.contains("Task 2"));
    }

    // ---------- Picocli-specific: argument parsing and exit codes ----------

    @Test
    @DisplayName("picocli: --help flag exits 0 and writes usage to stdout")
    void helpFlagPrintsUsage() {
        assertEquals(0, run("--help"));
        String out = console();
        assertTrue(out.contains("add"));
        assertTrue(out.contains("list"));
        assertTrue(out.contains("delete"));
    }

    @Test
    @DisplayName("picocli: unknown subcommand returns non-zero exit code")
    void unknownSubcommandFails() {
        assertNotEquals(0, run("unknown-command"));
    }

    @Test
    @DisplayName("picocli: successful commands return exit code 0")
    void successfulCommandsReturnZero() {
        assertEquals(0, run("add", "A task"));
        assertEquals(0, run("list"));
        assertEquals(0, run("mark-done", "1"));
        assertEquals(0, run("delete", "1"));
    }

    @Test
    @DisplayName("picocli: task-not-found errors return exit code 1")
    void taskNotFoundReturnsExitCode1() {
        int code = run("delete", "999");
        assertEquals(1, code);
    }

    @Test
    @DisplayName("picocli: type conversion errors (non-numeric ID) return exit code 2")
    void invalidTypeReturnsExitCode2() {
        assertEquals(2, run("delete", "not-a-number"));
    }
}
