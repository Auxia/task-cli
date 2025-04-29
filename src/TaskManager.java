import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private final Path FILE_PATH = Path.of("tasks.json");
    private static List<Task> tasks;

    public TaskManager() {
        tasks = loadTasks();
    }

    public List<Task> loadTasks() {
        List<Task> savedTasks = new ArrayList<>();

        if (!Files.exists(FILE_PATH)) {
            return new ArrayList<>();
        }

        try {
            String json = Files.readString(FILE_PATH);
            String[] taskList = json.replace("[", "").replace("]", "").split(",");

            for (String task : taskList) {
                if(!task.endsWith("}")) {
                    task = task + "}";
                    savedTasks.add(Task.convertFromJson(task));
                } else {
                    savedTasks.add(Task.convertFromJson(task));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return savedTasks;
    }

    public void saveTasks(List<Task> tasks) {
        try (BufferedWriter writer = new BufferedWriter( new FileWriter(FILE_PATH.toFile()))) {
            for (Task task : tasks) {
                writer.write(task.convertToJson());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addTask(String description) {
        Task newTask = new Task(description);
        tasks.add(newTask);
        System.out.println("Task Created Successfully (ID: " + newTask.getId() + ")");
    }

    public void updateTask(int id, String newDescription) {
        Task task = getTask(id);
        task.updateDescription(newDescription);
    }

    public void removeTask(int id) {
        Task task = getTask(id);
        tasks.remove(task);
    }

    public void markTaskAsToDo(int id) {
        Task task = getTask(id);
        task.markToDo();
    }

    public void markTaskAsInProgress(int id) {
        Task task = getTask(id);
        task.markInProgress();
    }

    public void markTaskAsDone(int id) {
        Task task = getTask(id);
        task.markDone();
    }

    public Task getTask(int id) {
        return tasks.stream().filter(task -> task.getId() == id).findFirst().orElseThrow(() -> new IllegalArgumentException("Task with ID: " + id + "does not exist!"));
    }
}
