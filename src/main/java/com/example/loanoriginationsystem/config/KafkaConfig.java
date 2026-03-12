package com.example.loanoriginationsystem.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.loan-events:loan-events}")
    private String loanEventsTopic;

    @Value("${kafka.topic-partitions.loan-events:3}")
    private int partitions;

    @Value("${kafka.topic-replication-factor.loan-events:1}")
    private short replicationFactor;

    @Bean
    public NewTopic loanEventsTopic() {
        return TopicBuilder.name(loanEventsTopic)
            .partitions(partitions)
            .replicas(replicationFactor)
            .build();
    }
}