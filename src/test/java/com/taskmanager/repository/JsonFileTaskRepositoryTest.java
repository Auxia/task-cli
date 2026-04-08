package com.taskmanager.repository;

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
import java.util.concurrent.TimeUnit;

class JsonFileTaskRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonFileTaskRepository repo;
    private Path testFile;

    @BeforeEach
    void setUp() {
        testFile = tempDir.resolve("test-tasks.json");
        repo = new JsonFileTaskRepository(testFile);
    }

    // ---------- Round-trip persistence ----------

    @Test
    @DisplayName("save and persist: tasks survive a full reload")
    void saveAndPersistRoundTrip() {
        Task t1 = repo.save(new Task(repo.nextId(), "First task"));
        Task t2 = repo.save(new Task(repo.nextId(), "Second task"));
        repo.save(new Task(t2.id(), t2.description(), TaskStatus.DONE, t2.createdAt(), t2.updatedAt()));
        repo.persist();

        JsonFileTaskRepository reloaded = new JsonFileTaskRepository(testFile);
        assertEquals(2, reloaded.count());
        assertTrue(reloaded.exists(t1.id()));
        assertEquals(TaskStatus.DONE, reloaded.findById(t2.id()).orElseThrow().status());
    }

    @Test
    @DisplayName("nextId after reload continues from max saved ID")
    void nextIdContinuesAfterReload() {
        repo.save(new Task(repo.nextId(), "Task"));  // id=1
        repo.save(new Task(repo.nextId(), "Task"));  // id=2
        repo.persist();

        JsonFileTaskRepository reloaded = new JsonFileTaskRepository(testFile);
        assertEquals(3, reloaded.nextId());
    }

    // ---------- Path validation ----------

    @Test
    @DisplayName("Rejects relative path that traverses above working directory")
    void rejectsPathTraversal() {
        assertThrows(IllegalArgumentException.class,
                () -> new JsonFileTaskRepository(Path.of("../../etc/passwd")));
    }

    @Test
    @DisplayName("Rejects path with traversal embedded mid-path")
    void rejectsEmbeddedTraversal() {
        assertThrows(IllegalArgumentException.class,
                () -> new JsonFileTaskRepository(Path.of("data/../../etc/shadow")));
    }

    @Test
    @DisplayName("Accepts absolute path")
    void acceptsAbsolutePath(@TempDir Path dir) {
        assertDoesNotThrow(() -> new JsonFileTaskRepository(dir.resolve("tasks.json")));
    }

    @Test
    @DisplayName("Accepts relative path within working directory")
    void acceptsRelativePathInWorkingDirectory() {
        assertDoesNotThrow(() -> JsonFileTaskRepository.validatePath(Path.of("tasks.json")));
    }

    @Test
    @DisplayName("Rejects path traversal via tasks.file system property")
    void rejectsPathTraversalViaSystemProperty() {
        System.setProperty("tasks.file", "../../etc/passwd");
        try {
            assertThrows(IllegalArgumentException.class, JsonFileTaskRepository::new);
        } finally {
            System.clearProperty("tasks.file");
        }
    }

    // ---------- File I/O edge cases ----------

    @Test
    @DisplayName("Corrupted JSON with no backup throws descriptive RuntimeException")
    void corruptedJsonThrowsOnLoad() throws IOException {
        Files.writeString(testFile, "{invalid json");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new JsonFileTaskRepository(testFile));
        assertTrue(ex.getMessage().contains("Failed to load tasks"),
                "Expected 'Failed to load tasks' in message but got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Corrupted primary file recovers from .bak when backup is valid")
    void corruptedPrimaryRecoverFromBackup() throws IOException {
        repo.save(new Task(repo.nextId(), "First task"));
        repo.save(new Task(repo.nextId(), "Second task"));
        repo.persist();
        repo.persist(); // second persist: .bak now holds a valid copy

        Files.writeString(testFile, "{invalid json");

        JsonFileTaskRepository recovered = new JsonFileTaskRepository(testFile);
        assertEquals(2, recovered.count());
        assertEquals("First task", recovered.findById(1).orElseThrow().description());
        assertEquals("Second task", recovered.findById(2).orElseThrow().description());
    }

    @Test
    @DisplayName("Both primary and backup corrupted throws RuntimeException")
    void bothFilesCorruptedThrows() throws IOException {
        Path backup = testFile.resolveSibling(testFile.getFileName() + ".bak");
        Files.writeString(testFile, "{invalid json");
        Files.writeString(backup, "{also invalid}");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new JsonFileTaskRepository(testFile));
        assertTrue(ex.getMessage().contains("Both tasks file and backup are unreadable"),
                "Expected 'Both tasks file and backup are unreadable' but got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Empty file written to disk loads as empty task list")
    void emptyFileOnDiskLoadsEmpty() throws IOException {
        Files.writeString(testFile, "");
        JsonFileTaskRepository loaded = new JsonFileTaskRepository(testFile);
        assertEquals(0, loaded.count());
    }

    @Test
    @DisplayName("Missing parent directory: construction succeeds, persist throws RuntimeException")
    void missingParentDirectoryFailsOnPersist() {
        Path path = tempDir.resolve("nonexistent/tasks.json");
        JsonFileTaskRepository r = assertDoesNotThrow(() -> new JsonFileTaskRepository(path));
        r.save(new Task(r.nextId(), "A task"));
        RuntimeException ex = assertThrows(RuntimeException.class, r::persist);
        assertTrue(ex.getMessage().contains("Failed to save tasks"),
                "Expected 'Failed to save tasks' in message but got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Read-only parent directory causes persist to throw RuntimeException")
    void readOnlyDirectoryThrowsOnPersist() throws IOException {
        assumeFalse("root".equals(System.getProperty("user.name")),
                "Skipped: setReadOnly() has no effect when running as root");
        Path roDir = Files.createDirectory(tempDir.resolve("ro"));
        Path path = roDir.resolve("tasks.json");
        JsonFileTaskRepository r = new JsonFileTaskRepository(path);
        r.save(new Task(r.nextId(), "A task"));
        r.persist();
        roDir.toFile().setReadOnly();
        try {
            assertThrows(RuntimeException.class, r::persist);
        } finally {
            roDir.toFile().setWritable(true);
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("10,000 tasks persist and reload without OOM or timeout")
    void largeTaskFileHandledCorrectly() {
        for (int i = 0; i < 10_000; i++) {
            repo.save(new Task(repo.nextId(), "Task " + i));
        }
        repo.persist();

        JsonFileTaskRepository reloaded = new JsonFileTaskRepository(testFile);
        assertEquals(10_000, reloaded.count());
        assertEquals(10_000, reloaded.findById(10_000).orElseThrow().id());
    }
}
