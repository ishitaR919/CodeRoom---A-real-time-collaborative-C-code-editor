package com.example.collabeditor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExecutionProducer {

    private static final Logger log = LoggerFactory.getLogger(ExecutionProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ExecutionProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void sendExecutionEvent(ExecutionEventDto event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.CODE_EXECUTION_TOPIC, event.getExecutionId().toString(), jsonPayload);
            log.info("Produced code execution event: {}", event.getExecutionId());
        } catch (Exception e) {
            log.error("Failed to produce code execution event: {}", event.getExecutionId(), e);
            throw new RuntimeException("Kafka message publishing failed", e);
        }
    }
}
