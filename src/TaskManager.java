import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class TaskManager {
    private final Path FILE_PATH = Path.of("tasks.json");
    private List<Task> tasks;

    public TaskManager() {
        this.tasks = loadTasks();
    }

    public List<Task> loadTasks() {

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

    public Task getTask(int id) {
        return tasks.stream().filter(task -> task.getId() == id).findFirst().orElseThrow(() -> new IllegalArgumentException("Task with ID: " + id + "does not exist!"));
    }
}
