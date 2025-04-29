import jdk.jshell.Snippet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Task {
    private static int lastId = 0;
    private int id;
    private String description;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

    public static Task convertFromJson(String json) {
        json = json.replace("{", "").replace("}", "").replace("\"","");
        String[] jsonParts = json.split(",");

        String id = jsonParts[0].split(":")[1].strip();
        String description = jsonParts[1].split(":")[1].strip();
        String statusStr = jsonParts[2].split(":")[1].strip();
        String createdAtStr = jsonParts[3].split("[a-z]:")[1].strip();
        String updatedAtStr = jsonParts[4].split("[a-z]:")[1].strip();

        TaskStatus status = TaskStatus.valueOf(statusStr.toUpperCase().replace("-", "_"));

        Task task = new Task(description);
        task.id = Integer.parseInt(id);
        task.status = status;
        task.createdAt = LocalDateTime.parse(createdAtStr, formatter);
        task.updatedAt = LocalDateTime.parse(updatedAtStr, formatter);

        if (Integer.parseInt(id) > lastId) {
            lastId = Integer.parseInt(id);
        }

        return task;
    }
}
