package com.fintech.payment_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "notification-service", url = "${service.notification.url}")
public interface NotificationServiceClient {

    @PostMapping("/notifications/send")
    void sendNotification(@RequestParam String userId,
                          @RequestParam String type,
                          @RequestParam Map<String, Object> data);
}