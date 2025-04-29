import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private final Path FILE_PATH = Path.of("tasks.json");
    private List<Task> tasks;

    public TaskManager() {
        this.tasks = loadTasks();
    }

    public List<Task> loadTasks() {
        List<Task> savedTasks = new ArrayList<>();

        if (!Files.exists(FILE_PATH)) {
            return new ArrayList<>();
        }

        try {
            String json = Files.readString(FILE_PATH);
            String[] taskList = json.replace("[", "").replace("]", "").split("},");

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

    public void saveTasks() {
        try (BufferedWriter writer = new BufferedWriter( new FileWriter(FILE_PATH.toFile()))) {
            writer.write("[\n");
            for (int i = 0; i < tasks.size(); i++) {
                writer.write(tasks.get(i).convertToJson());
                if(i < tasks.size() - 1) {
                    writer.write(",\n");
                }
            }
            writer.write("\n]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addTask(String description) {
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

    public void listTask(String filter) {
        for (Task task : tasks) {
            String status = task.getStatus().toString().strip();
            if (filter.equals("all") || status.equals(filter)) {
                System.out.println(task.toString());
            }
        }
    }

    public Task getTask(int id) {
        return tasks.stream().filter(task -> task.getId() == id).findFirst().orElseThrow(() -> new IllegalArgumentException("Task with ID: " + id + "does not exist!"));
    }
}
