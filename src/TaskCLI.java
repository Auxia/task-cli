import java.util.List;

public class TaskCLI {

    public static void main(String[] args) {

        TaskManager taskManager = new TaskManager();

        if (args.length < 1) {
            System.out.println("Usage: task-cli <command> [arguments]");
            System.exit(1);
        } else if (args.length == 1 && args[0].equals("help")) {
            System.out.println("Usage: task-cli <command> [arguments]");
            System.out.println(" ");
            System.out.println("Commands:");
            System.out.println("\tadd [argument]\t\t\t\t\tAdds a task [argument] to the task list");
            System.out.println("\tdelete [argument]\t\t\t\tDeletes a task [argument] from the task list");
            System.out.println("\tupdate [argument]\t\t\t\tUpdates a task [argument] from the task list");
            System.out.println("\tmark-in-progress [argument]\t\tMarks a task [argument] from the task list as in progress");
            System.out.println("\tmark-done [argument]\t\t\tMarks a task [argument] from the task list as done");
            System.out.println("\tmark-todo [argument]\t\t\tMarks a task [argument] from the task list as todo");
            System.out.println("\tlist [argument]\t\t\t\t\tLists all tasks in the task list with filter [argument]. If [argument] not specified then displays all tasks in the task list");
            System.exit(0);
        } else if (args.length > 1 && args[0].equals("add")) {
            String description = constructArgument(args);
            System.out.println("Adding task: " + description);
            taskManager.addTask(description);
            taskManager.saveTasks();
        } else if (args.length > 1 && args[0].equals("delete")) {
            System.out.println("Deleting task: " + args[1]);
            taskManager.removeTask(Integer.parseInt(args[1]));
            taskManager.saveTasks();
        } else if (args.length > 1 && args[0].equals("update")) {
            String description = constructArgument(args);
            System.out.println("Updating task: " + description);
            taskManager.updateTask(Integer.parseInt(args[1]), description);
            taskManager.saveTasks();
        } else if (args.length > 1 && args[0].equals("mark-in-progress")) {
            taskManager.markTaskAsInProgress(Integer.parseInt(args[1]));
            taskManager.saveTasks();
        } else if (args.length > 1 && args[0].equals("mark-done")) {
            taskManager.markTaskAsDone(Integer.parseInt(args[1]));
            taskManager.saveTasks();
        } else if (args.length > 1 && args[0].equals("mark-todo")) {
            taskManager.markTaskAsToDo(Integer.parseInt(args[1]));
            taskManager.saveTasks();
        }
        else if (args.length > 1 && args[0].equals("list")) {
            taskManager.listTask(args[1]);
        }
    }

    public static String constructArgument(String[] args) {
        StringBuilder description = new StringBuilder();

        for (int i = 1; i < args.length; i++) {
            description.append(args[i]).append(" ");
        }

        return description.toString().trim();
    }
}
