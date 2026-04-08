package com.taskmanager.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class JsonFileTaskRepository implements TaskRepository {

    private static final String DEFAULT_TASKS_FILE = "tasks.json";
    private static final Logger logger = LoggerFactory.getLogger(JsonFileTaskRepository.class);

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final Map<Integer, Task> tasks;
    private int nextId;

    public JsonFileTaskRepository() {
        this(resolvePathFromConfig());
    }

    public JsonFileTaskRepository(Path filePath) {
        Objects.requireNonNull(filePath, "File path cannot be null");
        this.filePath = validatePath(filePath);
        this.objectMapper = createObjectMapper();
        this.tasks = new LinkedHashMap<>();
        this.nextId = 1;
        loadTasks();
    }

    // ---------- TaskRepository ----------

    @Override
    public Task save(Task task) {
        tasks.put(task.id(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(int id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(t -> t.status() == status)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Task> delete(int id) {
        return Optional.ofNullable(tasks.remove(id));
    }

    @Override
    public int nextId() {
        return nextId++;
    }

    @Override
    public synchronized void persist() {
        try {
            List<Task> taskList = new ArrayList<>(tasks.values());
            taskList.sort(Comparator.comparingInt(Task::id));

            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(taskList);

            if (Files.exists(filePath)) {
                Path backup = filePath.resolveSibling(filePath.getFileName() + ".bak");
                Files.copy(filePath, backup, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Backed up tasks file to {}", backup);
            }

            Path tempFile = Files.createTempFile(
                    filePath.getParent() != null ? filePath.getParent() : Path.of("."),
                    "tasks", ".tmp");
            Files.writeString(tempFile, json);
            try {
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("Saved {} tasks to {}", taskList.size(), filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save tasks to file: " + filePath, e);
        }
    }

    @Override
    public int count() {
        return tasks.size();
    }

    @Override
    public boolean exists(int id) {
        return tasks.containsKey(id);
    }

    // ---------- Path validation ----------

    static Path validatePath(Path path) {
        Path normalized = path.normalize();
        if (!path.isAbsolute()) {
            Path workDir = Path.of("").toAbsolutePath().normalize();
            Path resolvedNormalized = normalized.toAbsolutePath().normalize();
            if (!resolvedNormalized.startsWith(workDir)) {
                throw new IllegalArgumentException(
                        "Task file path must not traverse above the working directory: " + path);
            }
        }
        return normalized;
    }

    // ---------- Internal I/O ----------

    private static Path resolvePathFromConfig() {
        String prop = System.getProperty("tasks.file");
        if (prop != null && !prop.isBlank()) return validatePath(Path.of(prop));
        String env = System.getenv("TASKS_FILE");
        if (env != null && !env.isBlank()) return validatePath(Path.of(env));
        return Path.of(DEFAULT_TASKS_FILE);
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private void loadTasks() {
        if (!Files.exists(filePath)) {
            logger.info("Tasks file does not exist: {}", filePath);
            return;
        }
        try {
            loadFrom(filePath);
        } catch (IOException e) {
            logger.warn("Failed to load {}, attempting backup recovery...", filePath, e);
            Path backup = filePath.resolveSibling(filePath.getFileName() + ".bak");
            if (!Files.exists(backup)) {
                throw new RuntimeException("Failed to load tasks and no backup found: " + filePath, e);
            }
            try {
                loadFrom(backup);
                logger.info("Recovered {} tasks from backup: {}", tasks.size(), backup);
            } catch (IOException ex) {
                throw new RuntimeException("Both tasks file and backup are unreadable: " + filePath, ex);
            }
        }
    }

    private void loadFrom(Path path) throws IOException {
        String content = Files.readString(path);
        if (content.trim().isEmpty()) {
            logger.info("Tasks file is empty: {}", path);
            return;
        }
        List<Task> loadedTasks = objectMapper.readValue(content, new TypeReference<List<Task>>() {});
        for (Task task : loadedTasks) {
            tasks.put(task.id(), task);
            nextId = Math.max(nextId, task.id() + 1);
        }
        logger.info("Loaded {} tasks from {}", tasks.size(), path);
    }
}
