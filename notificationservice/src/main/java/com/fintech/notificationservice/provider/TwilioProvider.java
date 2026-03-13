package com.fintech.notificationservice.provider;

import com.fintech.notificationservice.model.Notification;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TwilioProvider {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @Value("${twilio.enabled:true}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (enabled && accountSid != null && authToken != null) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized successfully");
        }
    }

    public ProviderResult sendSms(String toPhoneNumber, String messageBody, Notification notification) {
        if (!enabled) {
            log.info("Twilio is disabled - skipping SMS to: {}", toPhoneNumber);
            return ProviderResult.builder()
                    .success(true)
                    .message("Twilio disabled (test mode)")
                    .providerMessageId("test-" + System.currentTimeMillis())
                    .build();
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody
            ).create();

            log.info("SMS sent successfully to {}: {}", toPhoneNumber, message.getSid());

            return ProviderResult.builder()
                    .success(true)
                    .providerMessageId(message.getSid())
                    .build();

        } catch (ApiException e) {
            log.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage(), e);

            return ProviderResult.builder()
                    .success(false)
                    .errorCode(String.valueOf(e.getCode()))
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