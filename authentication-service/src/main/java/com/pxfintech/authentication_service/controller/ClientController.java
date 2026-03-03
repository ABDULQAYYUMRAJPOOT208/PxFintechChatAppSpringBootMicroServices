package com.pxfintech.authentication_service.controller;

import com.pxfintech.authentication_service.dto.request.ClientRegistrationRequest;
import com.pxfintech.authentication_service.dto.response.ClientResponse;
import com.pxfintech.authentication_service.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Slf4j
public class ClientController {

    private final ClientService clientService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientResponse> registerClient(@Valid @RequestBody ClientRegistrationRequest request) {
        log.info("Registering new OAuth2 client: {}", request.getClientName());
        ClientResponse response = clientService.registerClient(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientResponse> getClient(@PathVariable String clientId) {
        ClientResponse response = clientService.getClientByClientId(clientId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        List<ClientResponse> clients = clientService.getAllClients();
        return ResponseEntity.ok(clients);
    }

    @PutMapping("/{clientId}/rotate-secret")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientResponse> rotateClientSecret(@PathVariable String clientId) {
        ClientResponse response = clientService.rotateClientSecret(clientId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{clientId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientResponse> updateClientStatus(
            @PathVariable String clientId,
            @RequestParam boolean active) {
        ClientResponse response = clientService.updateClientStatus(clientId, active);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClient(@PathVariable String clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.ok().build();
    }
}