package com.pxfintech.authentication_service.service;

import com.pxfintech.authentication_service.dto.request.ClientRegistrationRequest;
import com.pxfintech.authentication_service.dto.response.ClientResponse;
import com.pxfintech.authentication_service.exception.ClientNotFoundException;
import com.pxfintech.authentication_service.model.entity.Client;
import com.pxfintech.authentication_service.model.enums.ClientType;
import com.pxfintech.authentication_service.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ClientResponse registerClient(ClientRegistrationRequest request) {
        log.info("Registering new client: {}", request.getClientName());

        // Generate client ID and secret
        String clientId = generateClientId(request.getClientName());
        String clientSecret = generateClientSecret();

        Client client = Client.builder()
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientName(request.getClientName())
                .clientType(ClientType.valueOf(request.getClientType()))
                .grantTypes(request.getGrantTypes())
                .redirectUris(request.getRedirectUris())
                .scopes(request.getScopes())
                .authenticationMethods(request.getAuthenticationMethods())
                .accessTokenValidity(request.getAccessTokenValidity())
                .refreshTokenValidity(request.getRefreshTokenValidity())
                .requireProofKey(request.getRequireProofKey())
                .requireAuthorizationConsent(request.getRequireAuthorizationConsent())
                .isActive(true)
                .build();

        Client savedClient = clientRepository.save(client);

        log.info("Client registered successfully with ID: {}", savedClient.getClientId());

        return mapToResponse(savedClient, clientSecret);
    }

    @Override
    public ClientResponse getClientByClientId(String clientId) {
        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));

        return mapToResponse(client, null);
    }

    @Override
    public List<ClientResponse> getAllClients() {
        return clientRepository.findAll().stream()
                .map(client -> mapToResponse(client, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClientResponse rotateClientSecret(String clientId) {
        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));

        String newSecret = generateClientSecret();
        client.setClientSecret(passwordEncoder.encode(newSecret));
        client.setUpdatedAt(LocalDateTime.now());

        clientRepository.save(client);

        log.info("Client secret rotated for: {}", clientId);

        return mapToResponse(client, newSecret);
    }

    @Override
    @Transactional
    public ClientResponse updateClientStatus(String clientId, boolean active) {
        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));

        client.setIsActive(active);
        client.setUpdatedAt(LocalDateTime.now());

        clientRepository.save(client);

        log.info("Client status updated for {}: active={}", clientId, active);

        return mapToResponse(client, null);
    }

    @Override
    @Transactional
    public void deleteClient(String clientId) {
        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));

        // Soft delete by deactivating
        client.setIsActive(false);
        client.setUpdatedAt(LocalDateTime.now());
        clientRepository.save(client);

        log.info("Client deactivated: {}", clientId);
    }

    private String generateClientId(String clientName) {
        String prefix = clientName.toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-");
        String randomPart = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "-" + randomPart;
    }

    private String generateClientSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ClientResponse mapToResponse(Client client, String plainSecret) {
        return ClientResponse.builder()
                .id(client.getId())
                .clientId(client.getClientId())
                .clientSecret(plainSecret != null ? plainSecret : "[HIDDEN]")
                .clientName(client.getClientName())
                .clientType(client.getClientType().name())
                .grantTypes(client.getGrantTypes())
                .redirectUris(client.getRedirectUris())
                .scopes(client.getScopes())
                .authenticationMethods(client.getAuthenticationMethods())
                .accessTokenValidity(client.getAccessTokenValidity())
                .refreshTokenValidity(client.getRefreshTokenValidity())
                .requireProofKey(client.getRequireProofKey())
                .requireAuthorizationConsent(client.getRequireAuthorizationConsent())
                .isActive(client.getIsActive())
                .createdAt(client.getCreatedAt())
                .build();
    }
}