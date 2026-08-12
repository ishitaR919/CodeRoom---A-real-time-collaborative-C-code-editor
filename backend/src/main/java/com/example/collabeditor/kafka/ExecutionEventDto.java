package com.example.collabeditor.kafka;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExecutionEventDto {

    private UUID executionId;
    private UUID roomId;
    private UUID userId;
    private String language;
    private String code;
    private OffsetDateTime timestamp;

    public ExecutionEventDto() {}

    public ExecutionEventDto(UUID executionId, UUID roomId, UUID userId, String language, String code, OffsetDateTime timestamp) {
        this.executionId = executionId;
        this.roomId = roomId;
        this.userId = userId;
        this.language = language;
        this.code = code;
        this.timestamp = timestamp;
    }

    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}
