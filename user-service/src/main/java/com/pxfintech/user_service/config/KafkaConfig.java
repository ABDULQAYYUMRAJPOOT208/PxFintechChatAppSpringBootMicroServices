package com.pxfintech.user_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String USER_REGISTRATION_TOPIC = "user-registration";
    public static final String USER_STATUS_TOPIC = "user-status";

    @Bean
    public NewTopic userRegistrationTopic() {
        return TopicBuilder.name(USER_REGISTRATION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userStatusTopic() {
        return TopicBuilder.name(USER_STATUS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
