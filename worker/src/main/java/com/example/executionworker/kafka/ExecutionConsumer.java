package com.example.executionworker.kafka;

import com.example.executionworker.model.ExecutionEvent;
import com.example.executionworker.runner.DockerRunnerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ExecutionConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExecutionConsumer.class);
    private final DockerRunnerService dockerRunnerService;
    private final ObjectMapper objectMapper;

    public ExecutionConsumer(DockerRunnerService dockerRunnerService) {
        this.dockerRunnerService = dockerRunnerService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "code-execution", groupId = "execution-worker-group")
    public void consume(String message) {
        log.info("Received Kafka message from topic 'code-execution'");
        try {
            ExecutionEvent event = objectMapper.readValue(message, ExecutionEvent.class);
            log.info("Processing execution request for executionId: {}", event.getExecutionId());
            dockerRunnerService.executeCodeAndReport(event);
        } catch (Exception e) {
            log.error("Failed to parse and execute Kafka event message: {}", message, e);
        }
    }
}
