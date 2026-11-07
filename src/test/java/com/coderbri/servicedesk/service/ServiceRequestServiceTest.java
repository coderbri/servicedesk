
package com.coderbri.servicedesk.service;

import com.coderbri.servicedesk.entity.RequestPriority;
import com.coderbri.servicedesk.entity.RequestStatus;
import com.coderbri.servicedesk.entity.ServiceRequest;
import com.coderbri.servicedesk.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @file: test/.../ServiceRequestServiceTest.java
 * @description: Unit tests for service request creation, retrieval, updates, and status transitions.
 * Uses a mocked repository to verify service behavior without a database.
 */
@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock
    private ServiceRequestRepository repository;

    private ServiceRequestService service;

    @BeforeEach
    void setUp() {
        service = new ServiceRequestService(repository);
    }

    @Test
    void createRequest_savesRequest() {
        String title = "Laptop Wi-Fi issue";
        String description = "Unable to connect to the office network.";
        RequestPriority priority = RequestPriority.HIGH;

        when(repository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequest result = service.createRequest(
                title, description, priority);

        assertEquals(title, result.getTitle());
        assertEquals(description, result.getDescription());
        assertEquals(priority, result.getPriority());
        assertEquals(RequestStatus.OPEN, result.getStatus());

        verify(repository).save(any(ServiceRequest.class));
    }

    @Test
    void getAllRequests_returnsAllRequests() {
        ServiceRequest first = new ServiceRequest(
                "Laptop issue", "Laptop won't connect.", RequestPriority.HIGH);
        ServiceRequest second = new ServiceRequest(
                "Monitor issue", "Monitor is blank.", RequestPriority.MEDIUM);

        when(repository.findAll()).thenReturn(List.of(first, second));

        List<ServiceRequest> result = service.getAllRequests();

        assertEquals(2, result.size());
        assertEquals("Laptop issue", result.get(0).getTitle());
        assertEquals("Monitor issue", result.get(1).getTitle());

        verify(repository).findAll();
    }

    @Test
    void getRequestById_whenRequestExists_returnsRequest() {
        Long id = 1L;
        ServiceRequest request = new ServiceRequest(
                "Laptop issue", "Laptop won't connect.", RequestPriority.HIGH);

        when(repository.findById(id)).thenReturn(Optional.of(request));

        ServiceRequest result = service.getRequestById(id);

        assertSame(request, result);
        verify(repository).findById(id);
    }

    @Test
    void getRequestById_whenRequestDoesNotExist_throwsNotFound() {
        Long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getRequestById(id));

        assertEquals(404, exception.getStatusCode().value());
        verify(repository).findById(id);
    }

    @Test
    void updateRequest_withValidTransition_updatesRequest() {
        Long id = 1L;
        ServiceRequest request = new ServiceRequest(
                "Laptop issue", "Old description.", RequestPriority.MEDIUM);

        when(repository.findById(id)).thenReturn(Optional.of(request));
        when(repository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequest result = service.updateRequest(
                id,
                "Laptop Wi-Fi issue",
                "Unable to connect to the office network.",
                RequestPriority.HIGH,
                RequestStatus.IN_PROGRESS);

        assertEquals("Laptop Wi-Fi issue", result.getTitle());
        assertEquals(
                "Unable to connect to the office network.",
                result.getDescription());
        assertEquals(RequestPriority.HIGH, result.getPriority());
        assertEquals(RequestStatus.IN_PROGRESS, result.getStatus());

        verify(repository).findById(id);
        verify(repository).save(request);
    }

    @Test
    void updateRequest_withInvalidTransition_throwsBadRequest() {
        Long id = 1L;
        ServiceRequest request = new ServiceRequest(
                "Laptop issue", "Laptop won't connect.", RequestPriority.HIGH);

        when(repository.findById(id)).thenReturn(Optional.of(request));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateRequest(
                        id,
                        "Laptop issue",
                        "Laptop still won't connect.",
                        RequestPriority.HIGH,
                        RequestStatus.CLOSED));

        assertEquals(400, exception.getStatusCode().value());

        verify(repository, never()).save(any(ServiceRequest.class));
    }

    @ParameterizedTest(name = "{0} may transition to {1}")
    @MethodSource("validStatusTransitions")
    void updateRequest_withAllowedTransition_savesRequest(
            RequestStatus currentStatus,
            RequestStatus nextStatus) {

        Long id = 1L;
        ServiceRequest request = new ServiceRequest(
                "Laptop issue", "Laptop won't connect.", RequestPriority.HIGH);
        request.update(
                request.getTitle(), request.getDescription(),
                request.getPriority(), currentStatus);

        when(repository.findById(id)).thenReturn(Optional.of(request));
        when(repository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequest result = service.updateRequest(
                id, "Laptop issue", "Laptop won't connect.",
                RequestPriority.HIGH, nextStatus);

        assertEquals(nextStatus, result.getStatus());
        verify(repository).save(request);
    }

    static Stream<Arguments> validStatusTransitions() {
        return Stream.of(
                arguments(RequestStatus.OPEN, RequestStatus.OPEN),
                arguments(RequestStatus.OPEN, RequestStatus.IN_PROGRESS),
                arguments(RequestStatus.IN_PROGRESS, RequestStatus.IN_PROGRESS),
                arguments(RequestStatus.IN_PROGRESS, RequestStatus.RESOLVED),
                arguments(RequestStatus.RESOLVED, RequestStatus.RESOLVED),
                arguments(RequestStatus.RESOLVED, RequestStatus.CLOSED),
                arguments(RequestStatus.CLOSED, RequestStatus.CLOSED)
        );
    }

    @ParameterizedTest(name = "{0} may not transition to {1}")
    @MethodSource("invalidStatusTransitions")
    void updateRequest_withDisallowedTransition_doesNotSave(
            RequestStatus currentStatus,
            RequestStatus nextStatus) {

        Long id = 1L;
        ServiceRequest request = new ServiceRequest(
                "Laptop issue", "Laptop won't connect.", RequestPriority.HIGH);
        request.update(
                request.getTitle(), request.getDescription(),
                request.getPriority(), currentStatus);
        when(repository.findById(id)).thenReturn(Optional.of(request));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateRequest(
                        id, "Laptop issue", "Laptop won't connect.",
                        RequestPriority.HIGH, nextStatus));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals(currentStatus, request.getStatus());
        verify(repository, never()).save(any(ServiceRequest.class));
    }

    static Stream<Arguments> invalidStatusTransitions() {
        return Stream.of(
                arguments(RequestStatus.OPEN, RequestStatus.RESOLVED),
                arguments(RequestStatus.OPEN, RequestStatus.CLOSED),
                arguments(RequestStatus.IN_PROGRESS, RequestStatus.OPEN),
                arguments(RequestStatus.IN_PROGRESS, RequestStatus.CLOSED),
                arguments(RequestStatus.RESOLVED, RequestStatus.OPEN),
                arguments(RequestStatus.RESOLVED, RequestStatus.IN_PROGRESS),
                arguments(RequestStatus.CLOSED, RequestStatus.OPEN),
                arguments(RequestStatus.CLOSED, RequestStatus.IN_PROGRESS),
                arguments(RequestStatus.CLOSED, RequestStatus.RESOLVED)
        );
    }

    @Test
    void updateRequest_whenRequestDoesNotExist_throwsNotFoundWithoutSaving() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateRequest(
                        999L, "Laptop issue", "Laptop won't connect.",
                        RequestPriority.HIGH, RequestStatus.IN_PROGRESS));

        assertEquals(404, exception.getStatusCode().value());
        verify(repository, never()).save(any(ServiceRequest.class));
    }
}
