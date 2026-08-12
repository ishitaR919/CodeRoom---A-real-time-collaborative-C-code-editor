package com.example.executionworker;

import com.example.executionworker.model.ExecutionEvent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ExecutionWorkerTests {

    @Test
    void testExecutionEventModel() {
        UUID execId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ExecutionEvent event = new ExecutionEvent();
        event.setExecutionId(execId);
        event.setRoomId(roomId);
        event.setUserId(userId);
        event.setLanguage("cpp");
        event.setCode("#include <iostream>\nint main(){ return 0; }");
        event.setTimestamp(OffsetDateTime.now());

        assertEquals(execId, event.getExecutionId());
        assertEquals("cpp", event.getLanguage());
        assertTrue(event.getCode().contains("iostream"));
    }
}
