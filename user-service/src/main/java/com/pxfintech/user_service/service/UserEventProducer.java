package com.pxfintech.user_service.service;

import com.pxfintech.user_service.config.KafkaConfig;
import com.pxfintech.user_service.dto.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void sendUserRegistrationEvent(UserEvent event) {
        log.info("Sending user registration event for user: {}", event.getUserId());
        kafkaTemplate.send(KafkaConfig.USER_REGISTRATION_TOPIC, event.getUserId(), event);
    }

    public void sendUserStatusEvent(String userId, String phoneNumber, String status) {
        log.info("Sending user status event: {} for user: {}", status, userId);
        UserEvent event = UserEvent.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .eventType(status)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaTemplate.send(KafkaConfig.USER_STATUS_TOPIC, userId, event);
    }
}
