package com.example.collabeditor.execution;

import java.util.UUID;

public class ExecutionResultDto {

    private UUID executionId;
    private UUID roomId;
    private UUID userId;
    private String status; // SUCCESS, COMPILATION_ERROR, RUNTIME_ERROR, TIMEOUT, SYSTEM_ERROR
    private String output;
    private String errorOutput;
    private long executionTimeMs;

    public ExecutionResultDto() {}

    public ExecutionResultDto(UUID executionId, UUID roomId, UUID userId, String status, String output, String errorOutput, long executionTimeMs) {
        this.executionId = executionId;
        this.roomId = roomId;
        this.userId = userId;
        this.status = status;
        this.output = output;
        this.errorOutput = errorOutput;
        this.executionTimeMs = executionTimeMs;
    }

    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getErrorOutput() { return errorOutput; }
    public void setErrorOutput(String errorOutput) { this.errorOutput = errorOutput; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
}
