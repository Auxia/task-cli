package com.taskmanager.repository;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    /** Inserts or replaces a task. Returns the task as stored. */
    Task save(Task task);

    Optional<Task> findById(int id);

    List<Task> findAll();

    List<Task> findByStatus(TaskStatus status);

    /** Removes the task with the given ID. Returns the removed task, or empty if not found. */
    Optional<Task> delete(int id);

    /** Returns the next available ID and advances the sequence. */
    int nextId();

    /** Flushes in-memory state to backing storage. No-op for in-memory implementations. */
    void persist();

    int count();

    boolean exists(int id);
}
