package com.fintech.notificationservice.provider;

import com.fintech.notificationservice.model.Notification;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class SendGridProvider {

    @Value("${sendgrid.api-key}")
    private String apiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.from-name}")
    private String fromName;

    @Value("${sendgrid.enabled:true}")
    private boolean enabled;

    public ProviderResult sendEmail(
            String toEmail,
            String subject,
            String htmlContent,
            String plainContent,
            Notification notification) {

        if (!enabled) {
            log.info("SendGrid is disabled - skipping email to: {}", toEmail);
            return ProviderResult.builder()
                    .success(true)
                    .message("SendGrid disabled (test mode)")
                    .providerMessageId("test-" + System.currentTimeMillis())
                    .build();
        }

        try {
            SendGrid sg = new SendGrid(apiKey);

            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, to, content);

            // Add plain text alternative
            if (plainContent != null) {
                mail.addContent(new Content("text/plain", plainContent));
            }

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email sent successfully to {}: {}", toEmail, response.getHeaders().get("X-Message-Id"));

                return ProviderResult.builder()
                        .success(true)
                        .providerMessageId(response.getHeaders().get("X-Message-Id"))
                        .build();
            } else {
                log.error("Failed to send email to {}: status={}", toEmail, response.getStatusCode());

                return ProviderResult.builder()
                        .success(false)
                        .errorCode(String.valueOf(response.getStatusCode()))
                        .errorMessage(response.getBody())
                        .build();
            }

        } catch (IOException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);

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