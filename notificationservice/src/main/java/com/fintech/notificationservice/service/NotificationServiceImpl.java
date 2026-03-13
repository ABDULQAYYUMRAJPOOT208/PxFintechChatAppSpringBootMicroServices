package com.fintech.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.notificationservice.dto.request.SendNotificationRequest;
import com.fintech.notificationservice.dto.response.NotificationResponse;
import com.fintech.notificationservice.exception.NotificationFailedException;
import com.fintech.notificationservice.model.*;
import com.fintech.notificationservice.provider.FcmProvider;
import com.fintech.notificationservice.provider.SendGridProvider;
import com.fintech.notificationservice.provider.TwilioProvider;
import com.fintech.notificationservice.repository.NotificationRepository;
import com.fintech.notificationservice.repository.TemplateRepository;
import com.fintech.notificationservice.template.TemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final TemplateRepository templateRepository;
    private final PreferenceService preferenceService;
    private final TemplateEngine templateEngine;
    private final FcmProvider fcmProvider;
    private final TwilioProvider twilioProvider;
    private final SendGridProvider sendGridProvider;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${notification.retry.max-attempts}")
    private int maxRetryAttempts;

    @Override
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        log.info("Sending notification to user: {} type: {}", request.getUserId(), request.getType());

        // Get user preferences
        UserPreference preferences = preferenceService.getUserPreferences(request.getUserId());

        // Determine channels to use
        List<Notification.NotificationChannel> channels = determineChannels(
                request, preferences, request.getType());

        // Process template if provided
        NotificationTemplate.ChannelContent processedContent = null;
        if (request.getTemplateId() != null) {
            processedContent = processTemplate(request.getTemplateId(), request.getVariables());
        }

        // Create notification record
        Notification notification = Notification.builder()
                .notificationId("NOTIF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userId(request.getUserId())
                .type(request.getType())
                .channel(channels.get(0)) // Primary channel
                .title(processedContent != null ? processedContent.getTitle() : request.getCustomTitle())
                .content(processedContent != null ? processedContent.getBody() : request.getCustomContent())
                .data(request.getData())
                .status(Notification.NotificationStatus.PENDING)
                .retryCount(0)
                .scheduledAt(request.getScheduledAt())
                .createdAt(Instant.now())
                .build();

        notification = notificationRepository.save(notification);

        // Send through each channel
        for (Notification.NotificationChannel channel : channels) {
            sendThroughChannel(notification, channel, processedContent, request.getVariables());
        }

        // Send real-time via WebSocket if in-app notification
        if (channels.contains(Notification.NotificationChannel.IN_APP)) {
            sendInAppNotification(notification);
        }

        return mapToResponse(notification);
    }

    @Override
    public List<NotificationResponse> sendBatchNotifications(List<SendNotificationRequest> requests) {
        log.info("Processing batch of {} notifications", requests.size());

        List<CompletableFuture<NotificationResponse>> futures = requests.stream()
                .map(request -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return sendNotification(request);
                    } catch (Exception e) {
                        log.error("Failed to send notification in batch: {}", e.getMessage());
                        return null;
                    }
                }))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public NotificationResponse getNotificationStatus(String notificationId) {
        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new NotificationFailedException("Notification not found: " + notificationId));

        return mapToResponse(notification);
    }

    @Override
    public void markAsRead(String userId, String notificationId) {
        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new NotificationFailedException("Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new NotificationFailedException("Notification does not belong to user");
        }

        notification.setStatus(Notification.NotificationStatus.READ);
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);

        log.info("Notification {} marked as read for user: {}", notificationId, userId);
    }

    @Override
    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndStatus(userId, Notification.NotificationStatus.DELIVERED);

        unread.forEach(notification -> {
            notification.setStatus(Notification.NotificationStatus.READ);
            notification.setReadAt(Instant.now());
        });

        notificationRepository.saveAll(unread);

        log.info("Marked {} notifications as read for user: {}", unread.size(), userId);
    }

    @Override
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndStatusIn(userId,
                List.of(Notification.NotificationStatus.DELIVERED));
    }

    private List<Notification.NotificationChannel> determineChannels(
            SendNotificationRequest request,
            UserPreference preferences,
            Notification.NotificationType type) {

        // If channel specified in request, use that
        if (request.getChannel() != null) {
            return Collections.singletonList(request.getChannel());
        }

        // Otherwise use user preferences
        UserPreference.ChannelPreference channelPref = preferences.getTypePreferences().get(type);

        if (channelPref != null && channelPref.isEnabled()) {
            List<Notification.NotificationChannel> channels = new ArrayList<>();

            // Add primary channel
            if (channelPref.getPrimaryChannel() != null) {
                channels.add(channelPref.getPrimaryChannel());
            }

            // Add additional channels
            if (channelPref.getChannels() != null) {
                channels.addAll(channelPref.getChannels());
            }

            return channels;
        }

        // Default to PUSH
        return Collections.singletonList(Notification.NotificationChannel.PUSH);
    }

    private NotificationTemplate.ChannelContent processTemplate(String templateId, Map<String, Object> variables) {
        NotificationTemplate template = templateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new NotificationFailedException("Template not found: " + templateId));

        return templateEngine.processChannelContent(
                template.getChannelContent().get(Notification.NotificationChannel.PUSH),
                variables != null ? variables : new HashMap<>()
        );
    }

    private void sendThroughChannel(
            Notification notification,
            Notification.NotificationChannel channel,
            NotificationTemplate.ChannelContent content,
            Map<String, Object> variables) {

        switch (channel) {
            case PUSH:
                sendPushNotification(notification, content, variables);
                break;
            case SMS:
                sendSmsNotification(notification, content, variables);
                break;
            case EMAIL:
                sendEmailNotification(notification, content, variables);
                break;
            case IN_APP:
                // Handled separately
                break;
        }
    }

    private void sendPushNotification(
            Notification notification,
            NotificationTemplate.ChannelContent content,
            Map<String, Object> variables) {

        try {
            // Get user's device tokens from User Service
            List<String> deviceTokens = getUserDeviceTokens(notification.getUserId());

            if (deviceTokens.isEmpty()) {
                log.warn("No device tokens found for user: {}", notification.getUserId());
                return;
            }

            FcmProvider.ProviderResult result;

            if (deviceTokens.size() == 1) {
                result = fcmProvider.sendPushNotification(
                        notification.getUserId(),
                        deviceTokens.get(0),
                        content.getTitle(),
                        content.getBody(),
                        content.getData(),
                        notification
                );
            } else {
                result = fcmProvider.sendMulticast(
                        notification.getUserId(),
                        deviceTokens,
                        content.getTitle(),
                        content.getBody(),
                        content.getData(),
                        notification
                );
            }

            updateNotificationStatus(notification, result, Notification.NotificationChannel.PUSH);

        } catch (Exception e) {
            log.error("Failed to send push notification: {}", e.getMessage());
            handleFailure(notification, Notification.NotificationChannel.PUSH, e.getMessage());
        }
    }

    private void sendSmsNotification(
            Notification notification,
            NotificationTemplate.ChannelContent content,
            Map<String, Object> variables) {

        try {
            // Get user's phone number from User Service
            String phoneNumber = getUserPhoneNumber(notification.getUserId());

            if (phoneNumber == null) {
                log.warn("No phone number found for user: {}", notification.getUserId());
                return;
            }

            TwilioProvider.ProviderResult result = twilioProvider.sendSms(
                    phoneNumber,
                    content.getSmsText() != null ? content.getSmsText() : content.getBody(),
                    notification
            );

            updateNotificationStatus(notification, result, Notification.NotificationChannel.SMS);

        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage());
            handleFailure(notification, Notification.NotificationChannel.SMS, e.getMessage());
        }
    }

    private void sendEmailNotification(
            Notification notification,
            NotificationTemplate.ChannelContent content,
            Map<String, Object> variables) {

        try {
            // Get user's email from User Service
            String email = getUserEmail(notification.getUserId());

            if (email == null) {
                log.warn("No email found for user: {}", notification.getUserId());
                return;
            }

            SendGridProvider.ProviderResult result = sendGridProvider.sendEmail(
                    email,
                    content.getSubject(),
                    content.getHtmlContent(),
                    content.getBody(),
                    notification
            );

            updateNotificationStatus(notification, result, Notification.NotificationChannel.EMAIL);

        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
            handleFailure(notification, Notification.NotificationChannel.EMAIL, e.getMessage());
        }
    }

    private void sendInAppNotification(Notification notification) {
        messagingTemplate.convertAndSendToUser(
                notification.getUserId(),
                "/queue/notifications",
                mapToResponse(notification)
        );
    }

    private void updateNotificationStatus(
            Notification notification,
            FcmProvider.ProviderResult result,
            Notification.NotificationChannel channel) {

        if (result.isSuccess()) {
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setProviderResponse(result.getProviderMessageId());
            notification.setSentAt(Instant.now());
        } else {
            handleFailure(notification, channel, result.getErrorMessage());
        }

        notificationRepository.save(notification);
    }

    private void updateNotificationStatus(
            Notification notification,
            TwilioProvider.ProviderResult result,
            Notification.NotificationChannel channel) {

        if (result.isSuccess()) {
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setProviderResponse(result.getProviderMessageId());
            notification.setSentAt(Instant.now());
        } else {
            handleFailure(notification, channel, result.getErrorMessage());
        }

        notificationRepository.save(notification);
    }

    private void updateNotificationStatus(
            Notification notification,
            SendGridProvider.ProviderResult result,
            Notification.NotificationChannel channel) {

        if (result.isSuccess()) {
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setProviderResponse(result.getProviderMessageId());
            notification.setSentAt(Instant.now());
        } else {
            handleFailure(notification, channel, result.getErrorMessage());
        }

        notificationRepository.save(notification);
    }

    private void handleFailure(Notification notification, Notification.NotificationChannel channel, String error) {
        log.error("Notification failed for user: {} via {}: {}",
                notification.getUserId(), channel, error);

        notification.setRetryCount(notification.getRetryCount() + 1);

        if (notification.getRetryCount() >= maxRetryAttempts) {
            notification.setStatus(Notification.NotificationStatus.FAILED);
        }

        notificationRepository.save(notification);
    }

    private List<String> getUserDeviceTokens(String userId) {
        // Call User Service to get device tokens
        // For now, return empty list
        return new ArrayList<>();
    }

    private String getUserPhoneNumber(String userId) {
        // Call User Service to get phone number
        // For now, return null
        return null;
    }

    private String getUserEmail(String userId) {
        // Call User Service to get email
        // For now, return null
        return null;
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .userId(notification.getUserId())
                .type(notification.getType().name())
                .channel(notification.getChannel().name())
                .title(notification.getTitle())
                .content(notification.getContent())
                .status(notification.getStatus().name())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .deliveredAt(notification.getDeliveredAt())
                .build();
    }
}