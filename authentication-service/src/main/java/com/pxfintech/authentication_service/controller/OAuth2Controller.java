package com.pxfintech.authentication_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class OAuth2Controller {

    @GetMapping("/userinfo")
    public Map<String, Object> userInfo(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> userInfo = new HashMap<>();

        if (principal instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) principal;
            userInfo.put("sub", oidcUser.getSubject());
            userInfo.put("preferred_username", oidcUser.getPreferredUsername());
            userInfo.put("name", oidcUser.getFullName());
            userInfo.put("email", oidcUser.getEmail());
            userInfo.put("phone_number", oidcUser.getPhoneNumber());
        } else {
            userInfo.put("sub", principal.getName());
            userInfo.put("name", principal.getAttributes().get("name"));
            userInfo.put("email", principal.getAttributes().get("email"));
        }

        return userInfo;
    }

    @GetMapping("/jwks")
    public Map<String, Object> jwks() {
        // This endpoint is automatically handled by Spring Authorization Server
        // This method exists just for documentation
        return Map.of("message", "JWKS endpoint - used for public key retrieval");
    }
}