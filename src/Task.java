import java.time.LocalDateTime;

public class Task {
    private static int lastId = 0;
    private final int id;
    private String description;
    private TaskStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Task(String description) {
        this.id = ++lastId;
        this.description = description;
        this.status = TaskStatus.TODO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public int getId() {
        return this.id;
    }

    public void markToDo() {
        this.status = TaskStatus.TODO;
        this.updatedAt = LocalDateTime.now();
    }

    public void markInProgress() {
        this.status = TaskStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    public void markDone() {
        this.status = TaskStatus.DONE;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
        this.updatedAt = LocalDateTime.now();
    }

    public String convertToJson() {
        return "{\"id\": " + this.id + ", \"description\": \"" + this.description + "\", \"status\": \"" + this.status.name() + "\","
                + "\"createdAt\": " + this.createdAt + ", \"updatedAt\": " + this.updatedAt + "}";
    }
}
