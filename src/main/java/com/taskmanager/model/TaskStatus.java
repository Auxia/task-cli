package com.taskmanager.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {
    TODO("todo", "TODO"),
    IN_PROGRESS("in-progress", "IN_PROGRESS"),
    DONE("done", "DONE");

    private final String displayName;
    private final String jsonValue;

    TaskStatus(String displayName, String jsonValue) {
        this.displayName = displayName;
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static TaskStatus fromJsonValue(String value) {
        for (TaskStatus status : values()) {
            if (status.jsonValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }

    public static TaskStatus fromDisplayName(String displayName) {
        for (TaskStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + displayName);
    }
}
