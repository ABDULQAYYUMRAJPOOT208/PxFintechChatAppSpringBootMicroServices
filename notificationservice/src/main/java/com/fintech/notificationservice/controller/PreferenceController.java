package com.fintech.notificationservice.controller;

import com.fintech.notificationservice.dto.request.UpdatePreferenceRequest;
import com.fintech.notificationservice.dto.response.PreferenceResponse;
import com.fintech.notificationservice.service.PreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
@Slf4j
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<PreferenceResponse> getPreferences(@RequestAttribute String userId) {
        log.info("REST request to get preferences for user: {}", userId);
        PreferenceResponse response = preferenceService.getPreferenceResponse(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<PreferenceResponse> updatePreferences(
            @RequestAttribute String userId,
            @Valid @RequestBody UpdatePreferenceRequest request) {
        log.info("REST request to update preferences for user: {}", userId);
        PreferenceResponse response = preferenceService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/channels/{channel}/opt-out")
    public ResponseEntity<Void> optOutChannel(
            @RequestAttribute String userId,
            @PathVariable String channel) {
        log.info("REST request to opt out of channel: {} for user: {}", channel, userId);
        preferenceService.optOutChannel(userId, channel);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/channels/{channel}/opt-in")
    public ResponseEntity<Void> optInChannel(
            @RequestAttribute String userId,
            @PathVariable String channel) {
        log.info("REST request to opt in to channel: {} for user: {}", channel, userId);
        preferenceService.optInChannel(userId, channel);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/channels/{channel}/status")
    public ResponseEntity<Boolean> isChannelOptedOut(
            @RequestAttribute String userId,
            @PathVariable String channel) {
        boolean optedOut = preferenceService.isChannelOptedOut(userId, channel);
        return ResponseEntity.ok(optedOut);
    }
}