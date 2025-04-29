# Installation

Clone the repository.

```shell
git clone https://github.com/Auxia/task-cli.git
cd task-cli
```

Compile the source files.
```shell
javac TaskCLI.java Task.java TaskManager.java TaskStatus.java
```

Now you can run the application by using the following command.
```shell
java TaskCLI <option> <argument>
```

# Usage
```shell
# Adding a new task
java TaskCLI add "Buy groceries"
# Output: Adding task: "Buy groceries"

# Updating a task
java TaskCLI update 1 "Buy groceries and cook dinner"
# Output: Task updated successfully (ID: 1)

# Deleting a task
java TaskCLI delete 1
# Output: Task deleted successfully (ID: 1)

# Marking a task as in progress
java TaskCLI mark-in-progress 1
# Output: Marking task 1 as in progress

# Marking a task as done
java TaskCLIApp mark-done 1
# Output: Marking task 1 as done

# Markign a task as todo
java TaskCLI mark-todo 1
# Output: Marking task 1 as todo

# Listing all tasks
java TaskCLI list
# Output: List of all tasks

# Listing tasks by status
java TaskCLI list TODO
java TaskCLI list IN_PROGRESS
java TaskCLI list DONE

```
