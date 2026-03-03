package com.pxfintech.authentication_service.service;

import com.pxfintech.authentication_service.dto.request.ClientRegistrationRequest;
import com.pxfintech.authentication_service.dto.response.ClientResponse;

import java.util.List;

public interface ClientService {

    ClientResponse registerClient(ClientRegistrationRequest request);

    ClientResponse getClientByClientId(String clientId);

    List<ClientResponse> getAllClients();

    ClientResponse rotateClientSecret(String clientId);

    ClientResponse updateClientStatus(String clientId, boolean active);

    void deleteClient(String clientId);
}