package com.coderbri.servicedesk.dto;

import com.coderbri.servicedesk.entity.RequestPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @file: dto/CreateServiceRequestRequest.java
 * @description: Input DTO for creating a service request; its validation annotations
 * check the title, description, and priority before the controller processes them.
 */
public class CreateServiceRequestRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @NotNull
    private RequestPriority priority;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public RequestPriority getPriority() {
        return priority;
    }
}
