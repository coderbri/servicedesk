package com.coderbri.servicedesk.controller;

import com.coderbri.servicedesk.dto.CreateServiceRequestRequest;
import com.coderbri.servicedesk.dto.ServiceRequestResponse;
import com.coderbri.servicedesk.entity.ServiceRequest;
import com.coderbri.servicedesk.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @file: controller/ServiceRequestController.java
 * @description: REST controller for the /api/requests endpoint; validates create
 * requests, calls the service, and returns the saved request with HTTP 201.
 */
@RestController
@RequestMapping("/api/requests")
public class ServiceRequestController {

    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> createRequest(
            @Valid @RequestBody CreateServiceRequestRequest request) {

        ServiceRequest savedRequest = service.createRequest(
                request.getTitle(),
                request.getDescription(),
                request.getPriority()
        );

        ServiceRequestResponse response =
                new ServiceRequestResponse(savedRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
