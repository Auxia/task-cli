package com.taskmanager.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum TaskStatus {
    TODO("todo", "TODO"),
    IN_PROGRESS("in-progress", "IN_PROGRESS"),
    DONE("done", "DONE");

    private final String displayName;
    private final String jsonValue;

    private static final Map<String, TaskStatus> BY_JSON_VALUE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(s -> s.jsonValue, s -> s));

    private static final Map<String, TaskStatus> BY_DISPLAY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(s -> s.displayName, s -> s));

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
        TaskStatus status = BY_JSON_VALUE.get(value);
        if (status == null) throw new IllegalArgumentException("Unknown status: " + value);
        return status;
    }

    public static TaskStatus fromDisplayName(String displayName) {
        TaskStatus status = BY_DISPLAY_NAME.get(displayName.toLowerCase());
        if (status == null) throw new IllegalArgumentException("Unknown status: " + displayName);
        return status;
    }
}
