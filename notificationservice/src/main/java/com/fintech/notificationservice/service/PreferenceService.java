package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.request.UpdatePreferenceRequest;
import com.fintech.notificationservice.dto.response.PreferenceResponse;
import com.fintech.notificationservice.model.UserPreference;
import com.fintech.notificationservice.repository.PreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;

    public UserPreference getUserPreferences(String userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    public PreferenceResponse updatePreferences(String userId, UpdatePreferenceRequest request) {
        UserPreference preferences = getUserPreferences(userId);

        if (request.getTypePreferences() != null) {
            preferences.setTypePreferences(request.getTypePreferences());
        }

        if (request.getQuietHours() != null) {
            preferences.setQuietHours(request.getQuietHours());
        }

        if (request.getOptedOutChannels() != null) {
            preferences.setOptedOutChannels(request.getOptedOutChannels());
        }

        preferences.setUpdatedAt(Instant.now());

        UserPreference saved = preferenceRepository.save(preferences);

        return mapToResponse(saved);
    }

    public PreferenceResponse getPreferenceResponse(String userId) {
        return mapToResponse(getUserPreferences(userId));
    }

    public void optOutChannel(String userId, String channel) {
        UserPreference preferences = getUserPreferences(userId);

        if (preferences.getOptedOutChannels() == null) {
            preferences.setOptedOutChannels(new java.util.ArrayList<>());
        }

        if (!preferences.getOptedOutChannels().contains(channel)) {
            preferences.getOptedOutChannels().add(channel);
            preferences.setUpdatedAt(Instant.now());
            preferenceRepository.save(preferences);
        }
    }

    public void optInChannel(String userId, String channel) {
        UserPreference preferences = getUserPreferences(userId);

        if (preferences.getOptedOutChannels() != null) {
            preferences.getOptedOutChannels().remove(channel);
            preferences.setUpdatedAt(Instant.now());
            preferenceRepository.save(preferences);
        }
    }

    public boolean isChannelOptedOut(String userId, String channel) {
        UserPreference preferences = getUserPreferences(userId);

        return preferences.getOptedOutChannels() != null &&
                preferences.getOptedOutChannels().contains(channel);
    }

    private UserPreference createDefaultPreferences(String userId) {
        UserPreference preferences = UserPreference.builder()
                .userId(userId)
                .typePreferences(new HashMap<>())
                .optedOutChannels(new java.util.ArrayList<>())
                .quietHours(UserPreference.QuietHours.builder()
                        .enabled(false)
                        .build())
                .updatedAt(Instant.now())
                .build();

        return preferenceRepository.save(preferences);
    }

    private PreferenceResponse mapToResponse(UserPreference preferences) {
        return PreferenceResponse.builder()
                .userId(preferences.getUserId())
                .typePreferences(preferences.getTypePreferences())
                .quietHours(preferences.getQuietHours())
                .optedOutChannels(preferences.getOptedOutChannels())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }
}