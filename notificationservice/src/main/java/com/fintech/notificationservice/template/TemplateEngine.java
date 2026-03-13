package com.fintech.notificationservice.template;

import com.fintech.notificationservice.model.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateEngine {

    private final SpringTemplateEngine templateEngine;
    private final VariableProcessor variableProcessor;

    public String processTemplate(String template, Map<String, Object> variables) {
        Context context = new Context();
        Map<String, Object> processedVariables = variableProcessor.processVariables(variables);
        context.setVariables(processedVariables);

        return templateEngine.process(template, context);
    }

    public NotificationTemplate.ChannelContent processChannelContent(
            NotificationTemplate.ChannelContent content,
            Map<String, Object> variables) {

        Map<String, Object> processedVars = variableProcessor.processVariables(variables);

        NotificationTemplate.ChannelContent processed = NotificationTemplate.ChannelContent.builder()
                .subject(processText(content.getSubject(), processedVars))
                .title(processText(content.getTitle(), processedVars))
                .body(processText(content.getBody(), processedVars))
                .smsText(processText(content.getSmsText(), processedVars))
                .htmlContent(processHtml(content.getHtmlContent(), processedVars))
                .build();

        if (content.getData() != null) {
            processed.setData(variableProcessor.processDataMap(content.getData(), processedVars));
        }

        return processed;
    }

    private String processText(String text, Map<String, Object> variables) {
        if (text == null) return null;

        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(text, context);
    }

    private String processHtml(String html, Map<String, Object> variables) {
        if (html == null) return null;

        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(html, context);
    }
}