package com.pxfintech.authentication_service.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class FederatedIdentityAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Handle OAuth2 login success
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauthToken.getPrincipal();

            Map<String, Object> userAttributes = new HashMap<>();

            if (oauth2User instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) oauth2User;
                userAttributes.put("sub", oidcUser.getSubject());
                userAttributes.put("email", oidcUser.getEmail());
                userAttributes.put("name", oidcUser.getFullName());
                userAttributes.put("provider", oauthToken.getAuthorizedClientRegistrationId());
            } else {
                userAttributes.put("sub", oauth2User.getName());
                userAttributes.put("attributes", oauth2User.getAttributes());
                userAttributes.put("provider", oauthToken.getAuthorizedClientRegistrationId());
            }

            // Store user info in session or create local user
            request.getSession().setAttribute("oauth2-user", userAttributes);

            // Redirect to frontend with success
            response.sendRedirect("http://localhost:3000/oauth2/success");
        }
    }
}