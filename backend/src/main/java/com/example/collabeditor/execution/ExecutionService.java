package com.example.collabeditor.execution;

import com.example.collabeditor.file.FileDto;
import com.example.collabeditor.file.FileService;
import com.example.collabeditor.kafka.ExecutionEventDto;
import com.example.collabeditor.kafka.ExecutionProducer;
import com.example.collabeditor.websocket.RoomSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);
    private final FileService fileService;
    private final ExecutionProducer executionProducer;
    private final RoomSessionManager sessionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExecutionService(FileService fileService, ExecutionProducer executionProducer, RoomSessionManager sessionManager) {
        this.fileService = fileService;
        this.executionProducer = executionProducer;
        this.sessionManager = sessionManager;
    }

    public Map<String, Object> requestExecution(UUID roomId, UUID userId, String customCode) {
        String codeToExecute = customCode;
        if (codeToExecute == null || codeToExecute.isBlank()) {
            FileDto.FileResponse file = fileService.getFileForRoom(roomId);
            codeToExecute = file.getContent();
        }

        UUID executionId = UUID.randomUUID();
        ExecutionEventDto event = new ExecutionEventDto(
                executionId,
                roomId,
                userId,
                "cpp",
                codeToExecute,
                OffsetDateTime.now()
        );

        executionProducer.sendExecutionEvent(event);

        return Map.of(
                "executionId", executionId,
                "status", "PENDING",
                "message", "Execution request submitted successfully"
        );
    }

    public void handleExecutionResult(ExecutionResultDto result) {
        log.info("Processing execution result for executionId: {}, roomId: {}, status: {}",
                result.getExecutionId(), result.getRoomId(), result.getStatus());

        try {
            Map<String, Object> wsPayload = Map.of(
                    "type", "EXECUTION_RESULT",
                    "executionId", result.getExecutionId(),
                    "roomId", result.getRoomId(),
                    "userId", result.getUserId(),
                    "status", result.getStatus(),
                    "output", result.getOutput() != null ? result.getOutput() : "",
                    "errorOutput", result.getErrorOutput() != null ? result.getErrorOutput() : "",
                    "executionTimeMs", result.getExecutionTimeMs()
            );

            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(wsPayload));
            sessionManager.broadcastToRoom(result.getRoomId().toString(), textMessage, null);
        } catch (Exception e) {
            log.error("Failed to broadcast execution result over WebSocket for room {}", result.getRoomId(), e);
        }
    }
}
