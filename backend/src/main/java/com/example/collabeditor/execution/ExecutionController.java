package com.example.collabeditor.execution;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    public static class ExecuteCodeRequest {
        private String code;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    @PostMapping("/api/rooms/{roomId}/execute")
    public ResponseEntity<?> executeCode(
            @PathVariable UUID roomId,
            @RequestBody(required = false) ExecuteCodeRequest request,
            Authentication authentication) {
        try {
            UUID userId = (UUID) authentication.getPrincipal();
            String code = request != null ? request.getCode() : null;
            Map<String, Object> response = executionService.requestExecution(roomId, userId, code);
            return ResponseEntity.accepted().body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/internal/execution-result")
    public ResponseEntity<?> receiveExecutionResult(@RequestBody ExecutionResultDto result) {
        try {
            executionService.handleExecutionResult(result);
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
