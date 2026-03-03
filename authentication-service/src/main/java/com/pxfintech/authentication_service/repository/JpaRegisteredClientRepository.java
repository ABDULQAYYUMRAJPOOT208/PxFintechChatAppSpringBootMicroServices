package com.pxfintech.authentication_service.repository;

import com.pxfintech.authentication_service.model.entity.Client;
import com.pxfintech.authentication_service.repository.ClientRepository;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JpaRegisteredClientRepository(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;

        ClassLoader classLoader = JpaRegisteredClientRepository.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        this.objectMapper.registerModules(securityModules);
        this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        Client client = toEntity(registeredClient);
        clientRepository.save(client);
    }

    @Override
    public RegisteredClient findById(String id) {
        return clientRepository.findById(java.util.UUID.fromString(id))
                .map(this::toObject)
                .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId)
                .map(this::toObject)
                .orElse(null);
    }

    private RegisteredClient toObject(Client client) {
        Set<String> clientAuthenticationMethods = StringUtils.commaDelimitedListToSet(
                client.getAuthenticationMethods() != null ?
                        String.join(",", client.getAuthenticationMethods()) : "");
        Set<String> authorizationGrantTypes = StringUtils.commaDelimitedListToSet(
                client.getGrantTypes() != null ?
                        String.join(",", client.getGrantTypes()) : "");
        Set<String> redirectUris = StringUtils.commaDelimitedListToSet(
                client.getRedirectUris() != null ?
                        String.join(",", client.getRedirectUris()) : "");
        Set<String> clientScopes = StringUtils.commaDelimitedListToSet(
                client.getScopes() != null ?
                        String.join(",", client.getScopes()) : "");

        RegisteredClient.Builder builder = RegisteredClient.withId(client.getId().toString())
                .clientId(client.getClientId())
                .clientIdIssuedAt(client.getCreatedAt())
                .clientSecret(client.getClientSecret())
                .clientName(client.getClientName());

        clientAuthenticationMethods.forEach(authenticationMethod ->
                builder.clientAuthenticationMethod(new ClientAuthenticationMethod(authenticationMethod)));

        authorizationGrantTypes.forEach(grantType ->
                builder.authorizationGrantType(new AuthorizationGrantType(grantType)));

        builder.redirectUris(uris -> uris.addAll(redirectUris));
        builder.scopes(scopes -> scopes.addAll(clientScopes));

        ClientSettings.Builder clientSettingsBuilder = ClientSettings.builder();
        clientSettingsBuilder.requireAuthorizationConsent(client.getRequireAuthorizationConsent() != null
                ? client.getRequireAuthorizationConsent() : true);
        clientSettingsBuilder.requireProofKey(client.getRequireProofKey() != null
                ? client.getRequireProofKey() : false);
        builder.clientSettings(clientSettingsBuilder.build());

        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();
        if (client.getAccessTokenValidity() != null) {
            tokenSettingsBuilder.accessTokenTimeToLive(
                    java.time.Duration.ofSeconds(client.getAccessTokenValidity()));
        }
        if (client.getRefreshTokenValidity() != null) {
            tokenSettingsBuilder.refreshTokenTimeToLive(
                    java.time.Duration.ofSeconds(client.getRefreshTokenValidity()));
        }
        tokenSettingsBuilder.reuseRefreshTokens(false);
        builder.tokenSettings(tokenSettingsBuilder.build());

        return builder.build();
    }

    private Client toEntity(RegisteredClient registeredClient) {
        List<String> clientAuthenticationMethods = new ArrayList<>(
                registeredClient.getClientAuthenticationMethods().stream()
                        .map(ClientAuthenticationMethod::getValue)
                        .toList());

        List<String> authorizationGrantTypes = new ArrayList<>(
                registeredClient.getAuthorizationGrantTypes().stream()
                        .map(AuthorizationGrantType::getValue)
                        .toList());

        List<String> redirectUris = new ArrayList<>(registeredClient.getRedirectUris());
        List<String> clientScopes = new ArrayList<>(registeredClient.getScopes());

        Client client = new Client();
        if (registeredClient.getId() != null && !registeredClient.getId().isEmpty()) {
            client.setId(java.util.UUID.fromString(registeredClient.getId()));
        }
        client.setClientId(registeredClient.getClientId());
        client.setClientSecret(registeredClient.getClientSecret());
        client.setClientName(registeredClient.getClientName());
        client.setAuthenticationMethods(clientAuthenticationMethods);
        client.setGrantTypes(authorizationGrantTypes);
        client.setRedirectUris(redirectUris);
        client.setScopes(clientScopes);

        ClientSettings clientSettings = registeredClient.getClientSettings();
        client.setRequireProofKey(clientSettings.isRequireProofKey());
        client.setRequireAuthorizationConsent(clientSettings.isRequireAuthorizationConsent());

        TokenSettings tokenSettings = registeredClient.getTokenSettings();
        client.setAccessTokenValidity((int) tokenSettings.getAccessTokenTimeToLive().getSeconds());
        client.setRefreshTokenValidity((int) tokenSettings.getRefreshTokenTimeToLive().getSeconds());

        client.setIsActive(true);

        return client;
    }
}