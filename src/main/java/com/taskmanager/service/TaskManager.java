package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.repository.TaskRepository;

import java.util.List;
import java.util.Objects;

/**
 * Service layer: business operations on tasks. All storage is delegated to
 * a {@link TaskRepository}; this class contains no I/O or serialisation logic.
 */
public class TaskManager {

    private final TaskRepository repository;

    public TaskManager(TaskRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    public Task addTask(String description) {
        Task task = new Task(repository.nextId(), description);
        return repository.save(task);
    }

    public Task updateTask(int id, String newDescription) {
        Task updated = findOrThrow(id).updateDescription(newDescription);
        return repository.save(updated);
    }

    public Task removeTask(int id) {
        return repository.delete(id)
                .orElseThrow(() -> new IllegalArgumentException("Task with ID " + id + " not found"));
    }

    public Task updateTaskStatus(int id, TaskStatus status) {
        Task updated = findOrThrow(id).updateStatus(status);
        return repository.save(updated);
    }

    public List<Task> getAllTasks()                   { return repository.findAll(); }
    public List<Task> getTasksByStatus(TaskStatus s) { return repository.findByStatus(s); }
    public Task getTaskById(int id)                  { return findOrThrow(id); }
    public boolean taskExists(int id)                { return repository.exists(id); }
    public int getTaskCount()                        { return repository.count(); }
    public void saveTasks()                          { repository.persist(); }

    private Task findOrThrow(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task with ID " + id + " not found"));
    }
}
