# Task CLI

A simple command-line task management application built with Java. Add, manage, and track your tasks efficiently from the terminal.

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Installation

Clone the repository and build the application:

```shell
git clone https://github.com/Auxia/task-cli.git
cd task-cli
mvn clean package
```

## Usage

### Adding a Task
```shell
java -jar target/task-cli.jar add "Buy groceries"
# Output: Task added successfully (ID: 1)
```

### Updating a Task
```shell
java -jar target/task-cli.jar update 1 "Buy organic groceries"
# Output: Task updated successfully (ID: 1)
```

### Deleting a Task
```shell
java -jar target/task-cli.jar delete 1
# Output: Task deleted successfully (ID: 1)
```

### Marking Tasks
```shell
# Mark as in progress
java -jar target/task-cli.jar mark-in-progress 1

# Mark as done
java -jar target/task-cli.jar mark-done 1

# Mark as todo
java -jar target/task-cli.jar mark-todo 1
```

### Listing Tasks
```shell
# List all tasks
java -jar target/task-cli.jar list

# List tasks by status
java -jar target/task-cli.jar list todo
java -jar target/task-cli.jar list in-progress
java -jar target/task-cli.jar list done
```

### Help
```shell
java -jar target/task-cli.jar help
```

## Commands

| Command | Description | Example |
|---------|-------------|---------|
| `add` | Add a new task | `add "Study Java"` |
| `update` | Update task description | `update 1 "Study Spring Boot"` |
| `delete` | Delete a task | `delete 1` |
| `mark-todo` | Mark task as TODO | `mark-todo 1` |
| `mark-in-progress` | Mark task as IN_PROGRESS | `mark-in-progress 1` |
| `mark-done` | Mark task as DONE | `mark-done 1` |
| `list` | List all tasks or filter by status | `list` or `list done` |
| `help` | Show help message | `help` |

## Data Storage

Tasks are automatically saved in `tasks.json` in the current directory.

## Testing

Run the test suite:
```shell
mvn test
```

---

This project was created as a practice exercise for Java OOP and file handling, based on the [roadmap.sh task tracker project](https://roadmap.sh/projects/task-tracker).
