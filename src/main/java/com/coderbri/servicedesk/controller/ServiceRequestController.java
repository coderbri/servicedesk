package com.coderbri.servicedesk.controller;

import com.coderbri.servicedesk.dto.CreateServiceRequestRequest;
import com.coderbri.servicedesk.dto.ServiceRequestResponse;
import com.coderbri.servicedesk.dto.UpdateServiceRequestRequest;
import com.coderbri.servicedesk.entity.ServiceRequest;
import com.coderbri.servicedesk.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @file: controller/ServiceRequestController.java
 * @description: REST controller for the /api/requests endpoint; validates request
 * payloads, delegates create, read, and update operations, and returns their responses.
 */
@RestController
@RequestMapping("/api/requests")
public class ServiceRequestController {

    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    /** Creates a service request and returns the saved representation. */
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

    /** Returns all saved service requests. */
    @GetMapping
    public ResponseEntity<List<ServiceRequestResponse>> getAllRequests() {
        List<ServiceRequestResponse> responses = service.getAllRequests()
                .stream()
                .map(ServiceRequestResponse::new)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /** Returns one saved service request by ID, or responds with 404 if absent. */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> getResponseById(@PathVariable Long id) {
        ServiceRequest request = service.getRequestById(id);

        return ResponseEntity.ok(new ServiceRequestResponse(request));
    }

    /** Updates a saved request by ID and returns its updated representation. */
    @PutMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequestRequest request) {

        ServiceRequest updatedRequest = service.updateRequest(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getStatus()
        );

        return ResponseEntity.ok(
                new ServiceRequestResponse(updatedRequest)
        );
    }
}
