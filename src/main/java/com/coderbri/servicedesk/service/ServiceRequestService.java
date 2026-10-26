package com.coderbri.servicedesk.service;

import com.coderbri.servicedesk.entity.RequestPriority;
import com.coderbri.servicedesk.entity.ServiceRequest;
import com.coderbri.servicedesk.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;

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

    public ServiceRequest createRequest(
            String title,
            String description,
            RequestPriority priority) {

        ServiceRequest request =
                new ServiceRequest(title, description, priority);

        return repository.save(request);
    }
}
