package com.fintech.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PublicEndpoints {

    @Value("${public.endpoints}")
    private List<String> endpoints;

    public List<String> getEndpoints() {
        return endpoints;
    }

    public boolean isPublic(String path) {
        return endpoints.stream().anyMatch(path::startsWith);
    }
}