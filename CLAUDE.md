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

The project is a Maven-based Java 25 CLI application structured in four packages under `com.taskmanager`:

### Model layer
- **`Task`** (`model/Task.java`) — immutable Java record. Equality is ID-only (entity identity, not value equality), so `equals`/`hashCode` ignore all fields except `id`. Update methods (`updateDescription`, `updateStatus`) return new instances. Description is trimmed and capped at `MAX_DESCRIPTION_LENGTH = 10_000`.
- **`TaskStatus`** (`model/TaskStatus.java`) — enum with two string representations: `displayName` (CLI-facing: `todo`, `in-progress`, `done`) and `jsonValue` (JSON-serialized: `TODO`, `IN_PROGRESS`, `DONE`). Both lookup maps are built once at class load for O(1) access.

### Repository layer
- **`TaskRepository`** (`repository/TaskRepository.java`) — interface defining the storage contract (`save`, `findById`, `findAll`, `findByStatus`, `delete`, `nextId`, `persist`, `count`, `exists`).
- **`JsonFileTaskRepository`** (`repository/JsonFileTaskRepository.java`) — file-backed implementation. Owns the in-memory `LinkedHashMap<Integer, Task>`, JSON serialisation (Jackson), atomic writes (write to temp → move), `.bak` backup on every persist, backup recovery on corrupt load, and path traversal protection. File location resolves from `-Dtasks.file` system property, then `TASKS_FILE` env var, then `tasks.json` in the working directory.
- **`InMemoryTaskRepository`** (`repository/InMemoryTaskRepository.java`) — `LinkedHashMap`-backed implementation with a no-op `persist()`. Used in unit tests and transient contexts; eliminates `@TempDir` overhead from business-logic tests.

### Service layer
- **`TaskManager`** (`service/TaskManager.java`) — pure business logic; no I/O or serialisation. Delegates all storage to a `TaskRepository`. Handles ID allocation (`nextId()`), find-or-throw, and the create/update/delete operations. Public API is identical to the pre-refactor class so `TaskCLI` required no subcommand changes.

### CLI layer
- **`TaskCLI`** (`cli/TaskCLI.java`) — Picocli entry point. Subcommands (`add`, `update`, `delete`, `list`, `mark-todo`, `mark-in-progress`, `mark-done`) are inner static classes sharing a single `TaskManager` instance. `buildCommandLine(TaskManager)` is package-private for use in tests. User-facing output goes to `System.out`; structured logging goes through SLF4J/Logback.

### Data flow
```
CLI invocation → Picocli subcommand → TaskManager (business logic)
    → TaskRepository.save/delete/findById → persist() → tasks.json
```
