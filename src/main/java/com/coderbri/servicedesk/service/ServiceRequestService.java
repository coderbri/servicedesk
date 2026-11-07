package com.coderbri.servicedesk.service;

import com.coderbri.servicedesk.entity.RequestPriority;
import com.coderbri.servicedesk.entity.RequestStatus;
import com.coderbri.servicedesk.entity.ServiceRequest;
import com.coderbri.servicedesk.repository.ServiceRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * @file: service/ServiceRequestService.java
 * @description: Application service that creates, retrieves, and updates service
 * requests, enforcing valid status transitions and delegating persistence to the repository.
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

    /** Updates a request, returning 404 when absent and 400 for an invalid status transition. */
    @Transactional
    public ServiceRequest updateRequest(
            Long id,
            String title,
            String description,
            RequestPriority priority,
            RequestStatus status) {

        ServiceRequest request = getRequestById(id);

        if (!isValidStatusTransition(request.getStatus(), status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status transition from "
                            + request.getStatus() + " to " + status
            );
        }

        request.update(title, description, priority, status);

        return repository.save(request);
    }

    /** Checks whether the requested status follows the service-request lifecycle. */
    private boolean isValidStatusTransition(
            RequestStatus current,
            RequestStatus next) {

        if (current == next) {
            return true;
        }

        return switch (current) {
            case OPEN -> next == RequestStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == RequestStatus.RESOLVED;
            case RESOLVED -> next == RequestStatus.CLOSED;
            case CLOSED -> false;
        };
    }

}
