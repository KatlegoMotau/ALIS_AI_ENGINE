package com.alis.backend;

import java.time.LocalDateTime;

public class LogResponse {

    private String userIdentifier;
    private String fileName;
    private String action;
    private LocalDateTime timestamp;

    public LogResponse(String userIdentifier, String fileName, String action, LocalDateTime timestamp) {
        this.userIdentifier = userIdentifier;
        this.fileName = fileName;
        this.action = action;
        this.timestamp = timestamp;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public String getFileName() {
        return fileName;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}