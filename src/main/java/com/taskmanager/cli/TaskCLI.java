package com.taskmanager.cli;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

@Command(name = "task-cli", mixinStandardHelpOptions = true, description = "Task CLI - Task Management Application")
public class TaskCLI {
    private final Logger logger = LoggerFactory.getLogger(TaskCLI.class);
    private final TaskManager taskManager;

    public TaskCLI() {
        this(new TaskManager());
    }

    // Keep existing constructor for tests and backward compatibility
    public TaskCLI(TaskManager taskManager) {
        this.taskManager = Objects.requireNonNull(taskManager);
    }

    public static void main(String[] args) {
        // Build root command and register subcommands with a shared TaskManager instance
        TaskManager tm = new TaskManager();
        TaskCLI root = new TaskCLI(tm);
        CommandLine cli = new CommandLine(root);

        // Register subcommands (instances so they share the same TaskManager)
        cli.addSubcommand("add", new AddCommand(tm));
        cli.addSubcommand("update", new UpdateCommand(tm));
        cli.addSubcommand("delete", new DeleteCommand(tm));
        cli.addSubcommand("mark-todo", new MarkTodoCommand(tm));
        cli.addSubcommand("mark-in-progress", new MarkInProgressCommand(tm));
        cli.addSubcommand("mark-done", new MarkDoneCommand(tm));
        cli.addSubcommand("list", new ListCommand(tm));

        if (args.length == 0) {
            cli.usage(System.out);
            return;
        }

        // Prefer picocli parsing; fall back to legacy execute for backward compatibility
        try {
            int exitCode = cli.execute(args);
            System.exit(exitCode);
        } catch (Exception ex) {
            // If picocli parsing fails (unknown command), fall back to legacy behavior
            TaskCLI wrapper = new TaskCLI(tm);
            try {
                wrapper.execute(args);
            } catch (Exception e) {
                LoggerFactory.getLogger(TaskCLI.class).error("Error executing legacy command: {}", e.getMessage());
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    // Preserve existing public API for tests
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
            logger.error("Command execution failed: {}", e.getMessage());
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

        // User-facing output
        System.out.printf("Task added successfully (ID: %d)%n", task.id());
        // Logging
        logger.info("Added task id={}", task.id());
    }

    private void handleUpdate(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Update command requires task ID and new description");
        }

        int id = parseId(args[1]);
        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        Task task = taskManager.updateTask(id, description);
        taskManager.saveTasks();

        System.out.printf("Task updated successfully (ID: %d)%n", task.id());
        logger.info("Updated task id={}", task.id());
    }

    private void handleDelete(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Delete command requires a task ID");
        }

        int id = parseId(args[1]);
        Task task = taskManager.removeTask(id);
        taskManager.saveTasks();

        System.out.printf("Task deleted successfully (ID: %d)%n", task.id());
        logger.info("Deleted task id={}", task.id());
    }

    private void handleMarkStatus(String[] args, TaskStatus status) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Status command requires a task ID");
        }

        int id = parseId(args[1]);
        Task task = taskManager.updateTaskStatus(id, status);
        taskManager.saveTasks();

        System.out.printf("Task %d marked as %s%n", task.id(), status.getDisplayName());
        logger.info("Task {} status changed to {}", task.id(), status.name());
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
                .sorted((t1, t2) -> Integer.compare(t1.id(), t2.id()))
                .forEach(task -> {
                    System.out.printf("%d. [%s] %s%n", task.id(), task.status().getDisplayName(), task.description());
                    logger.debug("Listing task id={} status={} desc={}", task.id(), task.status().name(), task.description());
                });
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
        logger.info("Task CLI - Task Management Application");
        logger.info("\nUsage: java TaskCLI <command> [arguments]");
        logger.info("\nCommands:");
        logger.info("  add <description>              Add a new task");
        logger.info("  update <id> <description>      Update task description");
        logger.info("  delete <id>                    Delete a task");
        logger.info("  mark-todo <id>                 Mark task as TODO");
        logger.info("  mark-in-progress <id>          Mark task as IN_PROGRESS");
        logger.info("  mark-done <id>                 Mark task as DONE");
        logger.info("  list [status]                  List all tasks or filter by status");
        logger.info("  help                           Show this help message");
        logger.info("\nExamples:");
        logger.info("  java TaskCLI add \"Buy groceries\"");
        logger.info("  java TaskCLI list todo");
        logger.info("  java TaskCLI mark-done 1");
    }

    // ---------- Picocli subcommands (instances share TaskManager) ----------

    @Command(name = "add", description = "Add a new task")
    static class AddCommand implements Callable<Integer> {
        private final TaskManager taskManager;

        @Parameters(arity = "1..*", description = "Task description")
        private List<String> descriptionParts;

        AddCommand(TaskManager tm) { this.taskManager = tm; }

        @Override
        public Integer call() {
            String description = String.join(" ", descriptionParts);
            Task task = taskManager.addTask(description);
            taskManager.saveTasks();
            System.out.printf("Task added successfully (ID: %d)%n", task.id());
            LoggerFactory.getLogger(AddCommand.class).info("Added task id={}", task.id());
            return 0;
        }
    }

    @Command(name = "update", description = "Update task description")
    static class UpdateCommand implements Callable<Integer> {
        private final TaskManager taskManager;

        @Parameters(index = "0", description = "Task ID")
        private int id;

        @Parameters(index = "1..*", description = "New description")
        private List<String> descriptionParts;

        UpdateCommand(TaskManager tm) { this.taskManager = tm; }

        @Override
        public Integer call() {
            String description = String.join(" ", descriptionParts);
            Task task = taskManager.updateTask(id, description);
            taskManager.saveTasks();
            System.out.printf("Task updated successfully (ID: %d)%n", task.id());
            LoggerFactory.getLogger(UpdateCommand.class).info("Updated task id={}", task.id());
            return 0;
        }
    }

    @Command(name = "delete", description = "Delete a task by ID")
    static class DeleteCommand implements Callable<Integer> {
        private final TaskManager taskManager;

        @Parameters(index = "0", description = "Task ID")
        private int id;

        DeleteCommand(TaskManager tm) { this.taskManager = tm; }

        @Override
        public Integer call() {
            Task task = taskManager.removeTask(id);
            taskManager.saveTasks();
            System.out.printf("Task deleted successfully (ID: %d)%n", task.id());
            LoggerFactory.getLogger(DeleteCommand.class).info("Deleted task id={}", task.id());
            return 0;
        }
    }

    @Command(name = "list", description = "List tasks or filter by status")
    static class ListCommand implements Callable<Integer> {
        private final TaskManager taskManager;

        @Parameters(index = "0", arity = "0..1", description = "Optional status: todo|in-progress|done")
        private String statusStr;

        ListCommand(TaskManager tm) { this.taskManager = tm; }

        @Override
        public Integer call() {
            List<Task> tasks;
            if (statusStr != null && !statusStr.isBlank()) {
                TaskStatus status = TaskStatus.fromDisplayName(statusStr.toLowerCase());
                tasks = taskManager.getTasksByStatus(status);
            } else {
                tasks = taskManager.getAllTasks();
            }

            if (tasks.isEmpty()) {
                System.out.println("No tasks found.");
                LoggerFactory.getLogger(ListCommand.class).info("No tasks to display");
                return 0;
            }

            tasks.stream()
                    .sorted((t1, t2) -> Integer.compare(t1.id(), t2.id()))
                    .forEach(task -> {
                        System.out.printf("%d. [%s] %s%n", task.id(), task.status().getDisplayName(), task.description());
                        LoggerFactory.getLogger(ListCommand.class).debug("Listing task id={} status={} desc={}", task.id(), task.status().name(), task.description());
                    });
            return 0;
        }
    }

    @Command(name = "mark-todo", description = "Mark task as TODO")
    static class MarkTodoCommand implements Callable<Integer> {
        private final TaskManager taskManager;
        @Parameters(index = "0", description = "Task ID")
        private int id;
        MarkTodoCommand(TaskManager tm) { this.taskManager = tm; }
        @Override public Integer call() {
            Task task = taskManager.updateTaskStatus(id, TaskStatus.TODO);
            taskManager.saveTasks();
            System.out.printf("Task %d marked as %s%n", task.id(), task.status().getDisplayName());
            LoggerFactory.getLogger(MarkTodoCommand.class).info("Task {} marked as {}", task.id(), task.status().name());
            return 0;
        }
    }

    @Command(name = "mark-in-progress", description = "Mark task as IN_PROGRESS")
    static class MarkInProgressCommand implements Callable<Integer> {
        private final TaskManager taskManager;
        @Parameters(index = "0", description = "Task ID")
        private int id;
        MarkInProgressCommand(TaskManager tm) { this.taskManager = tm; }
        @Override public Integer call() {
            Task task = taskManager.updateTaskStatus(id, TaskStatus.IN_PROGRESS);
            taskManager.saveTasks();
            System.out.printf("Task %d marked as %s%n", task.id(), task.status().getDisplayName());
            LoggerFactory.getLogger(MarkInProgressCommand.class).info("Task {} marked as {}", task.id(), task.status().name());
            return 0;
        }
    }

    @Command(name = "mark-done", description = "Mark task as DONE")
    static class MarkDoneCommand implements Callable<Integer> {
        private final TaskManager taskManager;
        @Parameters(index = "0", description = "Task ID")
        private int id;
        MarkDoneCommand(TaskManager tm) { this.taskManager = tm; }
        @Override public Integer call() {
            Task task = taskManager.updateTaskStatus(id, TaskStatus.DONE);
            taskManager.saveTasks();
            System.out.printf("Task %d marked as %s%n", task.id(), task.status().getDisplayName());
            LoggerFactory.getLogger(MarkDoneCommand.class).info("Task {} marked as {}", task.id(), task.status().name());
            return 0;
        }
    }
}
