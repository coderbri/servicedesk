
package com.coderbri.servicedesk.dto;

import com.coderbri.servicedesk.entity.RequestPriority;
import com.coderbri.servicedesk.entity.RequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @file: dto/UpdateServiceRequestRequest.java
 * @description: Request fields used to update an existing service request.
 */
public class UpdateServiceRequestRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @NotNull
    private RequestPriority priority;

    @NotNull
    private RequestStatus status;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public RequestPriority getPriority() {
        return priority;
    }

    public RequestStatus getStatus() {
        return status;
    }
}
