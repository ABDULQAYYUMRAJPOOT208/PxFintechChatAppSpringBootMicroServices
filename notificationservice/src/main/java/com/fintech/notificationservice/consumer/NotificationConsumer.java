package com.fintech.notificationservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.notificationservice.dto.request.SendNotificationRequest;
import com.fintech.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification-requests", groupId = "notification-service-group")
    public void consumeNotification(String message) {
        try {
            log.debug("Received notification request: {}", message);

            SendNotificationRequest request = objectMapper.readValue(
                    message, SendNotificationRequest.class);

            notificationService.sendNotification(request);

        } catch (Exception e) {
            log.error("Failed to process notification request: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "otp-events", groupId = "notification-service-group")
    public void consumeOtpEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            String phoneNumber = (String) event.get("phoneNumber");
            String otp = (String) event.get("otp");

            log.info("Processing OTP event for phone: {}", phoneNumber);

            // Send SMS notification
            SendNotificationRequest request = new SendNotificationRequest();
            request.setUserId(phoneNumber); // Phone number as temporary ID
            request.setType(Notification.NotificationType.OTP);
            request.setChannel(Notification.NotificationChannel.SMS);
            request.setVariables(Map.of("otp", otp));
            request.setTemplateId("otp-template");

            notificationService.sendNotification(request);

        } catch (Exception e) {
            log.error("Failed to process OTP event: {}", e.getMessage(), e);
        }
    }
}