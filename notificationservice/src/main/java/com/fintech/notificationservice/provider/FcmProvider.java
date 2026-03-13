package com.fintech.notificationservice.provider;

import com.fintech.notificationservice.model.Notification;
import com.fintech.notificationservice.model.UserPreference;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class FcmProvider {

    @Value("${firebase.enabled:true}")
    private boolean enabled;

    public ProviderResult sendPushNotification(
            String userId,
            String deviceToken,
            String title,
            String body,
            Map<String, String> data,
            Notification notification) {

        if (!enabled) {
            log.info("FCM is disabled - skipping push notification");
            return ProviderResult.builder()
                    .success(true)
                    .message("FCM disabled (test mode)")
                    .providerMessageId("test-" + System.currentTimeMillis())
                    .build();
        }

        try {
            // Build notification
            Notification.Builder notificationBuilder = Notification.builder()
                    .setTitle(title)
                    .setBody(body);

            // Build message
            Message.Builder messageBuilder = Message.builder()
                    .setNotification(notificationBuilder.build())
                    .putAllData(data)
                    .setToken(deviceToken);

            // Add Android specific config
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setClickAction("OPEN_ACTIVITY")
                            .setPriority(AndroidNotification.Priority.HIGH)
                            .build())
                    .build();

            messageBuilder.setAndroidConfig(androidConfig);

            // Add APNS config for iOS
            Aps aps = Aps.builder()
                    .setSound("default")
                    .setContentAvailable(true)
                    .build();

            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(aps)
                    .build();

            messageBuilder.setApnsConfig(apnsConfig);

            // Send message
            Message message = messageBuilder.build();
            String response = FirebaseMessaging.getInstance().send(message);

            log.info("Push notification sent successfully: {}", response);

            return ProviderResult.builder()
                    .success(true)
                    .providerMessageId(response)
                    .build();

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send push notification: {}", e.getMessage(), e);

            return ProviderResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .errorCode(e.getErrorCode() != null ? e.getErrorCode().name() : "UNKNOWN")
                    .build();
        }
    }

    public ProviderResult sendMulticast(
            String userId,
            java.util.List<String> deviceTokens,
            String title,
            String body,
            Map<String, String> data,
            Notification notification) {

        if (!enabled) {
            return ProviderResult.builder()
                    .success(true)
                    .message("FCM disabled (test mode)")
                    .build();
        }

        try {
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .addAllTokens(deviceTokens);

            BatchResponse response = FirebaseMessaging.getInstance()
                    .sendEachForMulticast(messageBuilder.build());

            log.info("Multicast push sent: success={}, failure={}",
                    response.getSuccessCount(), response.getFailureCount());

            return ProviderResult.builder()
                    .success(response.getFailureCount() == 0)
                    .providerMessageId("batch-" + System.currentTimeMillis())
                    .build();

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast push: {}", e.getMessage(), e);

            return ProviderResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class ProviderResult {
        private boolean success;
        private String providerMessageId;
        private String errorCode;
        private String errorMessage;
        private String message;
    }
}