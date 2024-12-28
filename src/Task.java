import java.time.LocalDateTime;
import java.util.Date;

public class Task {
    private static int lastId = 0;
    private int id;
    private String description;
    private int status; // Number from 0-2, describing, 0 - todo, 1 - in-progress, 2 - done.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Task(String description) {
        this.id = ++lastId;
        this.description = description;
        this.status = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public int getId() {
        return this.id;
    }

    public static int addTask(String description) {
        Task task = new Task(description);
        return task.getId();
    }
}
