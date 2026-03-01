package com.pxfintech.authentication_service.controller;

import com.pxfintech.authentication_service.dto.request.TokenRequest;
import com.pxfintech.authentication_service.dto.response.TokenIntrospectionResponse;
import com.pxfintech.authentication_service.dto.response.TokenResponse;
import com.pxfintech.authentication_service.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> createToken(@Valid @RequestBody TokenRequest request) {
        log.info("Token request with grant type: {}", request.getGrantType());

        TokenResponse response;

        switch (request.getGrantType()) {
            case "authorization_code":
                // Will implement with OAuth2
                response = tokenService.createAccessToken("user123", request.getClientId(), "read write");
                break;

            case "refresh_token":
                response = tokenService.refreshAccessToken(request.getRefreshToken(), request.getClientId());
                break;

            case "client_credentials":
                // Service-to-service authentication
                response = tokenService.createAccessToken("service", request.getClientId(), "service");
                break;

            default:
                throw new IllegalArgumentException("Unsupported grant type: " + request.getGrantType());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/introspect")
    public ResponseEntity<TokenIntrospectionResponse> introspectToken(@RequestParam String token) {
        log.info("Token introspection request");
        TokenIntrospectionResponse response = tokenService.introspectToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeToken(@RequestParam String token,
                                            @RequestParam(required = false) String userId,
                                            @RequestParam(required = false) String clientId) {
        log.info("Token revocation request");
        tokenService.revokeToken(token, userId, clientId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
        boolean isValid = tokenService.validateToken(token);
        return ResponseEntity.ok(isValid);
    }
}