# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```shell
# Build (produces target/task-cli-1.0.0-shaded.jar)
mvn clean package

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=TaskManagerTest

# Run a single test method
mvn test -Dtest=TaskManagerTest#addTask_shouldIncrementId

# Run the shaded JAR
java -jar target/task-cli-1.0.0-shaded.jar <command> [args]

# Override tasks file location
java -Dtasks.file=/path/to/tasks.json -jar target/task-cli-1.0.0-shaded.jar list
TASKS_FILE=/path/to/tasks.json java -jar target/task-cli-1.0.0-shaded.jar list
```

## Architecture

The project is a Maven-based Java 25 CLI application with four classes in `com.taskmanager`:

### Model layer
- **`Task`** (`model/Task.java`) — immutable Java record. Equality is ID-only (entity identity, not value equality), so `equals`/`hashCode` ignore all fields except `id`. Update methods (`updateDescription`, `updateStatus`) return new instances. Description is trimmed and capped at `MAX_DESCRIPTION_LENGTH = 10_000`.
- **`TaskStatus`** (`model/TaskStatus.java`) — enum with two string representations: `displayName` (CLI-facing: `todo`, `in-progress`, `done`) and `jsonValue` (JSON-serialized: `TODO`, `IN_PROGRESS`, `DONE`). Both lookup maps are built once at class load for O(1) access.

### Service layer
- **`TaskManager`** (`service/TaskManager.java`) — owns the in-memory `Map<Integer, Task>` and all I/O. Saves atomically (write to temp file → move), keeps a `.bak` backup on every save. Path traversal protection runs on construction — relative paths are rejected if they escape the working directory. File location resolves from `-Dtasks.file` system property, then `TASKS_FILE` env var, then `tasks.json` in the working directory.

### CLI layer
- **`TaskCLI`** (`cli/TaskCLI.java`) — dual-mode entry point. Picocli subcommands (`add`, `update`, `delete`, `list`, `mark-todo`, `mark-in-progress`, `mark-done`) are the primary path; a legacy `execute(String[])` method is preserved for backward compatibility with tests. All subcommands are inner static classes that receive a shared `TaskManager` instance. User-facing output goes to `System.out`; structured logging goes through SLF4J/Logback.

### Data flow
CLI invocation → Picocli subcommand → `TaskManager` mutates in-memory map → `saveTasks()` writes atomically to `tasks.json`.
