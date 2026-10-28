package com.coderbri.servicedesk.service;

import com.coderbri.servicedesk.entity.RequestPriority;
import com.coderbri.servicedesk.entity.ServiceRequest;
import com.coderbri.servicedesk.repository.ServiceRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * @file: service/ServiceRequestService.java
 * @description: Application service that creates service requests and delegates
 * persistence to the repository.
 */
@Service
public class ServiceRequestService {

    private final ServiceRequestRepository repository;

    public ServiceRequestService(ServiceRequestRepository repository) {
        this.repository = repository;
    }

    /** Creates and persists a service request. */
    public ServiceRequest createRequest(
            String title,
            String description,
            RequestPriority priority) {

        ServiceRequest request =
                new ServiceRequest(title, description, priority);

        return repository.save(request);
    }

    /** Retrieves all persisted service requests. */
    public List<ServiceRequest> getAllRequests() {
        return repository.findAll();
    }

    /** Retrieves one request by ID, throwing a 404 response when it is absent. */
    public ServiceRequest getRequestById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Service request not found: " + id
                ));
    }
}
