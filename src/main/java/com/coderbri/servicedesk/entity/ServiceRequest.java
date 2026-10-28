package com.coderbri.servicedesk.entity;

/**
 * @file: ServiceRequest.java
 * @description: JPA entity representing a service request stored in the database,
 * including its title, description, priority, status, generated ID, and creation time.
 */

import jakarta.persistence.*;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
@SequenceGenerator(
        name = "service_request_seq_gen",
        sequenceName = "service_requets_seq",
        allocationSize = 1
)

public class ServiceRequest {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_request_seq_gen"
    )
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestPriority priority;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ServiceRequest() {
        // Required by JPA.
    }

    public ServiceRequest(
            String title,
            String description,
            RequestPriority priority
    ) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = RequestStatus.OPEN;
    }

    /** Sets default status and creation time before the entity is first persisted. */
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = RequestStatus.OPEN;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Replaces the editable fields while retaining the request ID and creation time. */
    public void update(
            String title,
            String description,
            RequestPriority priority,
            RequestStatus status) {

        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
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
