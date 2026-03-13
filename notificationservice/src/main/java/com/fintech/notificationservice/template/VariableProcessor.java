package com.fintech.notificationservice.template;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class VariableProcessor {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a");

    public Map<String, Object> processVariables(Map<String, Object> variables) {
        if (variables == null) {
            return new HashMap<>();
        }

        Map<String, Object> processed = new HashMap<>(variables);

        // Process special variables
        if (variables.containsKey("amount")) {
            Object amount = variables.get("amount");
            if (amount instanceof BigDecimal) {
                processed.put("formattedAmount", CURRENCY_FORMAT.format(amount));
            }
        }

        if (variables.containsKey("date")) {
            Object date = variables.get("date");
            if (date instanceof Date) {
                processed.put("formattedDate", DATE_FORMAT.format(date));
                processed.put("formattedTime", TIME_FORMAT.format(date));
            }
        }

        if (variables.containsKey("userName")) {
            String userName = (String) variables.get("userName");
            processed.put("firstName", extractFirstName(userName));
        }

        return processed;
    }

    public Map<String, String> processDataMap(Map<String, String> data, Map<String, Object> variables) {
        Map<String, String> processed = new HashMap<>();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String value = entry.getValue();
            // Replace placeholders like {{variableName}}
            for (Map.Entry<String, Object> var : variables.entrySet()) {
                String placeholder = "{{" + var.getKey() + "}}";
                if (value.contains(placeholder)) {
                    value = value.replace(placeholder, String.valueOf(var.getValue()));
                }
            }
            processed.put(entry.getKey(), value);
        }

        return processed;
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "User";
        }
        String[] parts = fullName.split(" ");
        return parts[0];
    }
}