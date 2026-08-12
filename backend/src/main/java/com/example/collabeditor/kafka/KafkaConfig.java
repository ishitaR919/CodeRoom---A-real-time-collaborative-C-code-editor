package com.example.collabeditor.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String CODE_EXECUTION_TOPIC = "code-execution";

    @Bean
    public NewTopic codeExecutionTopic() {
        return TopicBuilder.name(CODE_EXECUTION_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
