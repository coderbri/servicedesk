package com.coderbri.servicedesk.repository;

import com.coderbri.servicedesk.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @file: repository/ServiceRequestRepository.java
 * @description: Spring Data JPA repository for saving and retrieving ServiceRequest
 * entities; JpaRepository supplies common database operations.
 */
public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequest, Long> {
}
