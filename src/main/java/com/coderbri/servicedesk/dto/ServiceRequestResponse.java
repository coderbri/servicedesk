package com.coderbri.servicedesk.dto;

import com.coderbri.servicedesk.entity.RequestPriority;
import com.coderbri.servicedesk.entity.RequestStatus;
import com.coderbri.servicedesk.entity.ServiceRequest;

import java.time.LocalDateTime;

/**
 * @file: dto/ServiceRequestResponse.java
 * @description: Output DTO containing the saved service request fields returned
 * to an API client, including its generated ID, status, and creation time.
 */
public class ServiceRequestResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final RequestStatus status;
    private final RequestPriority priority;
    private final LocalDateTime createdAt;

    public ServiceRequestResponse(ServiceRequest request) {
        this.id = request.getId();
        this.title = request.getTitle();
        this.description = request.getDescription();
        this.status = request.getStatus();
        this.priority = request.getPriority();
        this.createdAt = request.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public RequestPriority getPriority() {
        return priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
