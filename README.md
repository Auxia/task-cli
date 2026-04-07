# Task CLI

A small command-line task manager written in modern Java. This project was migrated to Maven, modernized (immutable model, thread-safe service, Picocli CLI, SLF4J logging) and packaged as a single runnable shaded JAR.

## Prerequisites

- Java 25 (JRE/JDK) or higher
- Maven 3.6 or higher

## Build

Clone and build the project with Maven:

```shell
git clone https://github.com/Auxia/task-cli.git
cd task-cli
mvn clean package
```

A shaded, runnable JAR is produced at:

```
target/task-cli-1.0.0-shaded.jar
```

## Usage

This CLI uses Picocli subcommands. Output is emitted via SLF4J (Logback) at INFO level by default.

Examples (run commands against the shaded JAR):

- Add a task
  ```shell
  java -jar target/task-cli-1.0.0-shaded.jar add "Buy groceries"
  ```

- Update a task
  ```shell
  java -jar target/task-cli-1.0.0-shaded.jar update 1 "Buy organic groceries"
  ```

- Delete a task
  ```shell
  java -jar target/task-cli-1.0.0-shaded.jar delete 1
  ```

- Change status
  ```shell
  java -jar target/task-cli-1.0.0-shaded.jar mark-in-progress 1
  java -jar target/task-cli-1.0.0-shaded.jar mark-done 1
  java -jar target/task-cli-1.0.0-shaded.jar mark-todo 1
  ```

- List tasks (optionally filter by status: todo, in-progress, done)
  ```shell
  java -jar target/task-cli-1.0.0-shaded.jar list
  java -jar target/task-cli-1.0.0-shaded.jar list done
  ```

- Show help
  ```shell
  java -jar target/task-cli-1.0.0-shaded.jar --help
  ```

## Configuration

- Tasks file path: by default tasks are stored in `tasks.json` in the working directory.
  Override with the environment variable `TASKS_FILE` or the system property `-Dtasks.file=path/to/file.json`.

- Logging: the app uses SLF4J with Logback (logback-classic). Add a `logback.xml` to `src/main/resources` or supply one on the classpath to customize formatting/levels. By default INFO-level messages are printed to the console.

## Docker

A simple Dockerfile is included. Build and run:

```shell
mvn package
docker build -t task-cli:1.0.0 .
# Persist tasks by mounting a volume for tasks.json
docker run --rm -v $(pwd)/tasks.json:/app/tasks.json task-cli:1.0.0 list
```

## Testing

Run unit tests locally:

```shell
mvn test
```

## Notes & Next steps

- The CLI is implemented with Picocli subcommands (add/update/delete/list/mark-*).
- Output is controlled via logging; if you prefer plain stdout messaging the behavior can be adjusted.
- Consider adding a `logback.xml` to set a user-friendly console pattern and log level, or enable file-based logging.

---

This project was originally based on the [roadmap.sh task tracker project](https://roadmap.sh/projects/task-tracker).