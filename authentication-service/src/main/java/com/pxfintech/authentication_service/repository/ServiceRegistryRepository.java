package com.pxfintech.authentication_service.repository;

import com.pxfintech.authentication_service.model.entity.ServiceRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRegistryRepository extends JpaRepository<ServiceRegistry, UUID> {

    Optional<ServiceRegistry> findByServiceName(String serviceName);

    Optional<ServiceRegistry> findByServiceId(String serviceId);

    boolean existsByServiceName(String serviceName);

    boolean existsByServiceId(String serviceId);

    @Modifying
    @Query("UPDATE ServiceRegistry s SET s.lastAccess = :lastAccess WHERE s.serviceId = :serviceId")
    int updateLastAccess(@Param("serviceId") String serviceId, @Param("lastAccess") LocalDateTime lastAccess);
}