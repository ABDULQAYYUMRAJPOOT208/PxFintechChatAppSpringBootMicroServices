package com.fintech.notificationservice.controller;

import com.fintech.notificationservice.dto.request.SendNotificationRequest;
import com.fintech.notificationservice.dto.response.NotificationResponse;
import com.fintech.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        log.info("REST request to send notification to user: {}", request.getUserId());
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<NotificationResponse>> sendBatchNotifications(
            @Valid @RequestBody List<SendNotificationRequest> requests) {
        log.info("REST request to send batch of {} notifications", requests.size());
        List<NotificationResponse> responses = notificationService.sendBatchNotifications(requests);
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @RequestAttribute String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("REST request to get notifications for user: {}", userId);
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse> getNotificationStatus(
            @PathVariable String notificationId) {
        log.info("REST request to get notification status: {}", notificationId);
        NotificationResponse response = notificationService.getNotificationStatus(notificationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @RequestAttribute String userId,
            @PathVariable String notificationId) {
        log.info("REST request to mark notification as read: {}", notificationId);
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestAttribute String userId) {
        log.info("REST request to mark all notifications as read for user: {}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(@RequestAttribute String userId) {
        log.info("REST request to get unread count for user: {}", userId);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }
}