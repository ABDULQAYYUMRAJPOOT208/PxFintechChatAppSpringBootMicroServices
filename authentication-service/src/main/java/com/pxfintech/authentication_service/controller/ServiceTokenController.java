package com.pxfintech.authentication_service.controller;

import com.pxfintech.authentication_service.dto.request.ServiceTokenRequest;
import com.pxfintech.authentication_service.dto.request.TokenExchangeRequest;
import com.pxfintech.authentication_service.dto.response.TokenExchangeResponse;
import com.pxfintech.authentication_service.dto.response.TokenResponse;
import com.pxfintech.authentication_service.service.ServiceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/service")
@RequiredArgsConstructor
@Slf4j
public class ServiceTokenController {

    private final ServiceTokenService serviceTokenService;

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> getServiceToken(@Valid @RequestBody ServiceTokenRequest request) {
        log.info("Service token request for: {}", request.getServiceName());
        TokenResponse response = serviceTokenService.generateServiceToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/exchange")
    public ResponseEntity<TokenExchangeResponse> exchangeToken(@Valid @RequestBody TokenExchangeRequest request) {
        log.info("Token exchange request");
        TokenExchangeResponse response = serviceTokenService.exchangeToken(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateServiceToken(
            @RequestParam String token,
            @RequestParam(required = false) String requiredScope,
            @RequestParam(required = false) String targetService) {
        boolean isValid = serviceTokenService.validateServiceToken(token, requiredScope, targetService);
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> registerService(
            @RequestParam String serviceName,
            @RequestParam String description,
            @RequestParam(required = false) String[] allowedScopes) {
        serviceTokenService.registerService(serviceName, description, allowedScopes);
        return ResponseEntity.ok("Service registered successfully");
    }

    @PostMapping("/{serviceName}/rotate-secret")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rotateServiceSecret(@PathVariable String serviceName) {
        serviceTokenService.rotateServiceSecret(serviceName);
        return ResponseEntity.ok("Service secret rotated successfully");
    }
}