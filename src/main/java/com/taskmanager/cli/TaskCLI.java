package com.taskmanager.cli;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.repository.JsonFileTaskRepository;
import com.taskmanager.service.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "task-cli", mixinStandardHelpOptions = true,
        description = "Task CLI - Task Management Application")
public class TaskCLI {

    public static void main(String[] args) {
        TaskManager tm = new TaskManager(new JsonFileTaskRepository());
        CommandLine cli = buildCommandLine(tm);
        if (args.length == 0) {
            cli.usage(System.out);
            return;
        }
        System.exit(cli.execute(args));
    }

    /**
     * Builds a fully wired CommandLine with all subcommands sharing the given TaskManager.
     * Package-private so tests can construct it directly without going through main().
     */
    static CommandLine buildCommandLine(TaskManager tm) {
        CommandLine cli = new CommandLine(new TaskCLI());
        cli.addSubcommand("add", new AddCommand(tm));
        cli.addSubcommand("update", new UpdateCommand(tm));
        cli.addSubcommand("delete", new DeleteCommand(tm));
        cli.addSubcommand("mark-todo", new MarkTodoCommand(tm));
        cli.addSubcommand("mark-in-progress", new MarkInProgressCommand(tm));
        cli.addSubcommand("mark-done", new MarkDoneCommand(tm));
        cli.addSubcommand("list", new ListCommand(tm));
        return cli;
    }

    // ---------- Subcommands ----------

    @Command(name = "add", description = "Add a new task")
    static class AddCommand implements Callable<Integer> {
        private static final Logger logger = LoggerFactory.getLogger(AddCommand.class);
        private final TaskManager taskManager;

        @Parameters(arity = "1..*", description = "Task description")
        private List<String> descriptionParts;

        AddCommand(TaskManager tm) { this.taskManager = tm; }

        @Override
        public Integer call() {
            Task task = taskManager.addTask(String.join(" ", descriptionParts));
            taskManager.saveTasks();
            System.out.printf("Task added successfully (ID: %d)%n", task.id());
            logger.info("Added task id={}", task.id());
            return 0;
        }
    }

    @Command(name = "update", description = "Update a task's description")
    static class UpdateCommand implements Callable<Integer> {
        private static final Logger logger = LoggerFactory.getLogger(UpdateCommand.class);
        private final TaskManager taskManager;

        @Parameters(index = "0", description = "Task ID")
        private int id;

        @Parameters(index = "1..*", description = "New description")
        private List<String> descriptionParts;

        UpdateCommand(TaskManager tm) { this.taskManager = tm; }

        @Override
        public Integer call() {
            Task task = taskManager.updateTask(id, String.join(" ", descriptionParts));
            taskManager.saveTasks();
            System.out.printf("Task updated successfully (ID: %d)%n", task.id());
            logger.info("Updated task id={}", task.id());
            return 0;
        }
    }

    @Command(name = "delete", description = "Delete a task by ID")
    static class DeleteCommand implements Callable<Integer> {
        private static final Logger logger = LoggerFactory.getLogger(DeleteCommand.class);
        private final TaskManager taskManager;

        @Parameters(index = "0", description = "Task ID")
        private int id;

        DeleteCommand(TaskManager tm) { this.taskManager = tm; }

        @Override
        public Integer call() {
            Task task = taskManager.removeTask(id);
            taskManager.saveTasks();
            System.out.printf("Task deleted successfully (ID: %d)%n", task.id());
            logger.info("Deleted task id={}", task.id());
            return 0;
        }
    }

    @Command(name = "list", description = "List tasks, optionally filtered by status (todo|in-progress|done)")
    static class ListCommand implements Callable<Integer> {
        private final TaskManager taskManager;

        @Parameters(index = "0", arity = "0..1", description = "Optional status filter: todo|in-progress|done")
        private String statusStr;

        ListCommand(TaskManager tm) { this.taskManager = tm; }

        @Override
        public Integer call() {
            List<Task> tasks = (statusStr != null && !statusStr.isBlank())
                    ? taskManager.getTasksByStatus(TaskStatus.fromDisplayName(statusStr.toLowerCase()))
                    : taskManager.getAllTasks();

            if (tasks.isEmpty()) {
                System.out.println("No tasks found.");
                return 0;
            }

            tasks.stream()
                    .sorted((a, b) -> Integer.compare(a.id(), b.id()))
                    .forEach(t -> System.out.printf("%d. [%s] %s%n",
                            t.id(), t.status().getDisplayName(), t.description()));
            return 0;
        }
    }

    @Command(name = "mark-todo", description = "Mark a task as TODO")
    static class MarkTodoCommand implements Callable<Integer> {
        private final TaskManager taskManager;
        @Parameters(index = "0", description = "Task ID") private int id;
        MarkTodoCommand(TaskManager tm) { this.taskManager = tm; }
        @Override public Integer call() { return markAs(taskManager, id, TaskStatus.TODO); }
    }

    @Command(name = "mark-in-progress", description = "Mark a task as IN_PROGRESS")
    static class MarkInProgressCommand implements Callable<Integer> {
        private final TaskManager taskManager;
        @Parameters(index = "0", description = "Task ID") private int id;
        MarkInProgressCommand(TaskManager tm) { this.taskManager = tm; }
        @Override public Integer call() { return markAs(taskManager, id, TaskStatus.IN_PROGRESS); }
    }

    @Command(name = "mark-done", description = "Mark a task as DONE")
    static class MarkDoneCommand implements Callable<Integer> {
        private final TaskManager taskManager;
        @Parameters(index = "0", description = "Task ID") private int id;
        MarkDoneCommand(TaskManager tm) { this.taskManager = tm; }
        @Override public Integer call() { return markAs(taskManager, id, TaskStatus.DONE); }
    }

    private static final Logger logger = LoggerFactory.getLogger(TaskCLI.class);

    private static int markAs(TaskManager tm, int id, TaskStatus status) {
        Task task = tm.updateTaskStatus(id, status);
        tm.saveTasks();
        System.out.printf("Task %d marked as %s%n", task.id(), status.getDisplayName());
        logger.info("Task {} marked as {}", task.id(), status.name());
        return 0;
    }
}
