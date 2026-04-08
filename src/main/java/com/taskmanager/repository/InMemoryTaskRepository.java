package com.taskmanager.repository;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory TaskRepository with no backing storage.
 * persist() is a no-op. Intended for use in tests and transient contexts.
 */
public class InMemoryTaskRepository implements TaskRepository {

    private final Map<Integer, Task> tasks = new LinkedHashMap<>();
    private int nextId = 1;

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
    public void persist() {
        // no-op
    }

    @Override
    public int count() {
        return tasks.size();
    }

    @Override
    public boolean exists(int id) {
        return tasks.containsKey(id);
    }
}
