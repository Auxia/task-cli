package com.taskmanager.cli;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.TaskManager;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TaskCLI {
    private final TaskManager taskManager;

    public TaskCLI() {
        this(new TaskManager());
    }

    public TaskCLI(TaskManager taskManager) {
        this.taskManager = Objects.requireNonNull(taskManager);
    }

    public static void main(String[] args) {
        TaskCLI cli = new TaskCLI();
        try {
            cli.execute(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    public void execute(String[] args) {
        if (args.length == 0) {
            showUsage();
            return;
        }

        String command = args[0].toLowerCase();

        try {
            switch (command) {
                case "add" -> handleAdd(args);
                case "update" -> handleUpdate(args);
                case "delete" -> handleDelete(args);
                case "mark-todo" -> handleMarkStatus(args, TaskStatus.TODO);
                case "mark-in-progress" -> handleMarkStatus(args, TaskStatus.IN_PROGRESS);
                case "mark-done" -> handleMarkStatus(args, TaskStatus.DONE);
                case "list" -> handleList(args);
                case "help" -> showUsage();
                default -> throw new IllegalArgumentException("Unknown command: " + command);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute command '" + command + "': " + e.getMessage(), e);
        }
    }

    private void handleAdd(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Add command requires a task description");
        }

        String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Task task = taskManager.addTask(description);
        taskManager.saveTasks();

        System.out.printf("Task added successfully (ID: %d)%n", task.getId());
    }

    private void handleUpdate(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Update command requires task ID and new description");
        }

        int id = parseId(args[1]);
        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        Task task = taskManager.updateTask(id, description);
        taskManager.saveTasks();

        System.out.printf("Task updated successfully (ID: %d)%n", task.getId());
    }

    private void handleDelete(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Delete command requires a task ID");
        }

        int id = parseId(args[1]);
        Task task = taskManager.removeTask(id);
        taskManager.saveTasks();

        System.out.printf("Task deleted successfully (ID: %d)%n", task.getId());
    }

    private void handleMarkStatus(String[] args, TaskStatus status) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Status command requires a task ID");
        }

        int id = parseId(args[1]);
        Task task = taskManager.updateTaskStatus(id, status);
        taskManager.saveTasks();

        System.out.printf("Task %d marked as %s%n", task.getId(), status.getDisplayName());
    }

    private void handleList(String[] args) {
        List<Task> tasks;

        if (args.length > 1) {
            TaskStatus status = parseStatus(args[1]);
            tasks = taskManager.getTasksByStatus(status);
        } else {
            tasks = taskManager.getAllTasks();
        }

        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }

        tasks.stream()
                .sorted((t1, t2) -> Integer.compare(t1.getId(), t2.getId()))
                .forEach(task -> System.out.printf("%d. [%s] %s%n",
                        task.getId(),
                        task.getStatus().getDisplayName(),
                        task.getDescription()));
    }

    private int parseId(String idStr) {
        try {
            int id = Integer.parseInt(idStr);
            if (id <= 0) {
                throw new IllegalArgumentException("Task ID must be positive");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid task ID: " + idStr);
        }
    }

    private TaskStatus parseStatus(String statusStr) {
        try {
            return TaskStatus.fromDisplayName(statusStr.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr + ". Valid options: todo, in-progress, done");
        }
    }

    private void showUsage() {
        System.out.println("Task CLI - Task Management Application");
        System.out.println("\nUsage: java TaskCLI <command> [arguments]");
        System.out.println("\nCommands:");
        System.out.println("  add <description>              Add a new task");
        System.out.println("  update <id> <description>      Update task description");
        System.out.println("  delete <id>                    Delete a task");
        System.out.println("  mark-todo <id>                 Mark task as TODO");
        System.out.println("  mark-in-progress <id>          Mark task as IN_PROGRESS");
        System.out.println("  mark-done <id>                 Mark task as DONE");
        System.out.println("  list [status]                  List all tasks or filter by status");
        System.out.println("  help                           Show this help message");
        System.out.println("\nExamples:");
        System.out.println("  java TaskCLI add \"Buy groceries\"");
        System.out.println("  java TaskCLI list todo");
        System.out.println("  java TaskCLI mark-done 1");
    }
}
