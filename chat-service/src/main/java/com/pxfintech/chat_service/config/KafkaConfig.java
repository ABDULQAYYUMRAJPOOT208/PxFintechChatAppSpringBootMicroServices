package com.pxfintech.chat_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.messages:chat-messages}")
    private String messagesTopic;

    @Value("${kafka.topic.delivery:chat-delivery}")
    private String deliveryTopic;

    @Value("${kafka.topic.typing:chat-typing}")
    private String typingTopic;

    @Bean
    public NewTopic messagesTopic() {
        return TopicBuilder.name(messagesTopic)
                .partitions(10)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic deliveryTopic() {
        return TopicBuilder.name(deliveryTopic)
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic typingTopic() {
        return TopicBuilder.name(typingTopic)
                .partitions(5)
                .replicas(1)
                .build();
    }
}