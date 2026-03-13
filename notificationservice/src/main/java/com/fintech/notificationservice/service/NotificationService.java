package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.request.SendNotificationRequest;
import com.fintech.notificationservice.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    NotificationResponse sendNotification(SendNotificationRequest request);

    List<NotificationResponse> sendBatchNotifications(List<SendNotificationRequest> requests);

    Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable);

    NotificationResponse getNotificationStatus(String notificationId);

    void markAsRead(String userId, String notificationId);

    void markAllAsRead(String userId);

    long getUnreadCount(String userId);
}