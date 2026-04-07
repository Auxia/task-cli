package com.taskmanager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskManager {
    private static final String DEFAULT_TASKS_FILE = "tasks.json";
    private final Logger logger = LoggerFactory.getLogger(TaskManager.class);

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final Map<Integer, Task> tasks;
    private final AtomicInteger nextId;

    public TaskManager() {
        this(resolvePathFromConfig());
    }

    private static Path resolvePathFromConfig() {
        String prop = System.getProperty("tasks.file");
        if (prop != null && !prop.isBlank()) return Path.of(prop);
        String env = System.getenv("TASKS_FILE");
        if (env != null && !env.isBlank()) return Path.of(env);
        return Path.of(DEFAULT_TASKS_FILE);
    }

    public TaskManager(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.objectMapper = createObjectMapper();
        this.tasks = new ConcurrentHashMap<>();
        this.nextId = new AtomicInteger(1);
        loadTasks();
    }

    private ObjectMapper createObjectMapper() {
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
            String content = Files.readString(filePath);
            if (content.trim().isEmpty()) {
                logger.info("Tasks file is empty: {}", filePath);
                return;
            }

            List<Task> loadedTasks = objectMapper.readValue(content, new TypeReference<List<Task>>() {});

            for (Task task : loadedTasks) {
                tasks.put(task.id(), task);
                nextId.set(Math.max(nextId.get(), task.id() + 1));
            }
            logger.info("Loaded {} tasks from {}", tasks.size(), filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tasks from file: " + filePath, e);
        }
    }

    public synchronized void saveTasks() {
        try {
            List<Task> taskList = new ArrayList<>(tasks.values());
            taskList.sort(Comparator.comparingInt(Task::id));

            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(taskList);

            // Backup existing file if present
            if (Files.exists(filePath)) {
                Path backup = filePath.resolveSibling(filePath.getFileName() + ".bak");
                Files.copy(filePath, backup, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Backed up tasks file to {}", backup);
            }

            // Atomic write to temp then move
            Path tempFile = Files.createTempFile(filePath.getParent() != null ? filePath.getParent() : Path.of("."), "tasks", ".tmp");
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

    public Task addTask(String description) {
        Task task = new Task(nextId.getAndIncrement(), description);
        tasks.put(task.id(), task);
        return task;
    }

    public Task updateTask(int id, String newDescription) {
        Task existing = getTaskById(id);
        Task updated = existing.updateDescription(newDescription);
        tasks.put(id, updated);
        return updated;
    }

    public Task removeTask(int id) {
        Task task = tasks.remove(id);
        if (task == null) {
            throw new IllegalArgumentException("Task with ID " + id + " not found");
        }
        return task;
    }

    public Task updateTaskStatus(int id, TaskStatus status) {
        Task existing = getTaskById(id);
        Task updated = existing.updateStatus(status);
        tasks.put(id, updated);
        return updated;
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.status() == status)
                .collect(Collectors.toList());
    }

    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Task with ID " + id + " not found");
        }
        return task;
    }

    public boolean taskExists(int id) {
        return tasks.containsKey(id);
    }

    public int getTaskCount() {
        return tasks.size();
    }
}
