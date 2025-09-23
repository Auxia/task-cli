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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TaskManager {
    private static final String TASKS_FILE = "tasks.json";
    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final Map<Integer, Task> tasks;
    private final AtomicInteger nextId;

    public TaskManager() {
        this(Path.of(TASKS_FILE));
    }

    public TaskManager(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.objectMapper = createObjectMapper();
        this.tasks = new HashMap<>();
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
            return;
        }

        try {
            String content = Files.readString(filePath);
            if (content.trim().isEmpty()) {
                return;
            }

            List<Task> loadedTasks = objectMapper.readValue(content, new TypeReference<List<Task>>() {});

            for (Task task : loadedTasks) {
                tasks.put(task.getId(), task);
                nextId.set(Math.max(nextId.get(), task.getId() + 1));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tasks from file: " + filePath, e);
        }
    }

    public void saveTasks() {
        try {
            List<Task> taskList = new ArrayList<>(tasks.values());
            taskList.sort(Comparator.comparing(Task::getId));

            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(taskList);
            Files.writeString(filePath, json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save tasks to file: " + filePath, e);
        }
    }

    public Task addTask(String description) {
        Task task = new Task(nextId.getAndIncrement(), description);
        tasks.put(task.getId(), task);
        return task;
    }

    public Task updateTask(int id, String newDescription) {
        Task task = getTaskById(id);
        task.updateDescription(newDescription);
        return task;
    }

    public Task removeTask(int id) {
        Task task = tasks.remove(id);
        if (task == null) {
            throw new IllegalArgumentException("Task with ID " + id + " not found");
        }
        return task;
    }

    public Task updateTaskStatus(int id, TaskStatus status) {
        Task task = getTaskById(id);
        task.updateStatus(status);
        return task;
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.getStatus() == status)
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
