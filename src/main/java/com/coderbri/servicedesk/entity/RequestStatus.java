package com.coderbri.servicedesk.entity;

/**
 * @file: entity/RequestStatus.java
 * @description: Allowed lifecycle states for a service request; the enum prevents
 * arbitrary status values and is stored by name on the entity.
 */
public enum RequestStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
