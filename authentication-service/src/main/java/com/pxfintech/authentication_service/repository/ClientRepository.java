package com.pxfintech.authentication_service.repository;

import com.pxfintech.authentication_service.model.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByClientId(String clientId);

    boolean existsByClientId(String clientId);
}