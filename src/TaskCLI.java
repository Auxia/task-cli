public class TaskCLI {

    public static void main(String[] args) {
        // Accept inputs from the CLI
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
            System.out.println("\tlist [argument]\t\t\t\t\tLists all tasks in the task list with filter [argument]. If [argument] not specified then displays all tasks in the task list");
            System.exit(0);
        }
    }
}
