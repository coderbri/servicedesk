
package com.coderbri.servicedesk.service;

import com.coderbri.servicedesk.entity.RequestPriority;
import com.coderbri.servicedesk.entity.RequestStatus;
import com.coderbri.servicedesk.entity.ServiceRequest;
import com.coderbri.servicedesk.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @file: service/ServiceRequestServiceTest.java
 * @description:
 * Service tests ask: “Does the business logic behave correctly?”
 */
@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    /* = TEST CLASS AND SETUP =
     * @ExtendWith(Mockito.class): tells JUnit to initialize Mockito for this test class.
     * @Mock: creates a substitute repository to test service. It doesn't connect to Oracle or save real data.
     * @BeforeEach runs setUp() before every test, creating a fresh service that uses the mock repository.
     * Mockito lets us control what the repository returns so we can focus on the testing the service's logic.
    */
    @Mock
    private ServiceRequestRepository repository;

    private ServiceRequestService service;

    @BeforeEach
    void setUp() {
        service = new ServiceRequestService(repository);
    }

    /* TEST 1: CREATING A REQUEST */
    @Test
    void createRequest_savesRequest() {

        /** = ARRANGE =
         * Define the request data and configure the mock.
         * With any(ServiceRequest.class), Mockito should match a call to save() with any ServiceRequest object.
         * thenAnswer(...) returns the same object passed into save(), rather than the default null.
         */
        String title = "Laptop Wi-Fi issue";
        String description = "Unable to connect to the office network.";
        RequestPriority priority = RequestPriority.HIGH;

        when(repository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        /** = ACT =
         * Calls the service.createRequest() to execute the real service method's business logic
         */
        ServiceRequest result = service.createRequest(
                title, description, priority);

        /** = ASSERT =
         * Checks that the returned request has the expected title, description, priority and initial OPEN status.
         * verify(...) confirms that the service attempted to save the request.
         */
        assertEquals(title, result.getTitle());
        assertEquals(description, result.getDescription());
        assertEquals(priority, result.getPriority());
        assertEquals(RequestStatus.OPEN, result.getStatus());

        verify(repository).save(any(ServiceRequest.class));
    }

    /* TEST 2: RETRIEVING ALL REQUESTS */
    @Test
    void getAllRequests_returnsAllRequests() {
        // Arrange: prepare two requests and configure the repository's result.
        ServiceRequest first = new ServiceRequest(
                "Laptop issue", "Laptop won't connect.", RequestPriority.HIGH);
        ServiceRequest second = new ServiceRequest(
                "Monitor issue", "Monitor is blank.", RequestPriority.MEDIUM);

        when(repository.findAll()).thenReturn(List.of(first, second));

        // Act: ask the service to retrieve all requests.
        List<ServiceRequest> result = service.getAllRequests();

        // Assert: confirm both requests were returned in the expected order.
        assertEquals(2, result.size());
        assertEquals("Laptop issue", result.get(0).getTitle());
        assertEquals("Monitor issue", result.get(1).getTitle());

        verify(repository).findAll();
    }

    /* TEST 3: RETRIEVE ONE EXISTING REQUEST */
    @Test
    void getRequestById_whenRequestExists_returnsRequest() {
        // Arrange: configure the repository to find a request for this ID.
        Long id = 1L;
        ServiceRequest request = new ServiceRequest(
                "Laptop issue", "Laptop won't connect.", RequestPriority.HIGH);

        when(repository.findById(id)).thenReturn(Optional.of(request));

        // Act: request the record using its ID.
        ServiceRequest result = service.getRequestById(id);

        // Assert: confirm the service returned the exact request we supplied.
        assertSame(request, result);
        verify(repository).findById(id);
    }

    /* TEST 4: RETRIEVING A NONEXISTENT REQUEST */
    @Test
    void getRequestById_whenRequestDoesNotExist_throwsNotFound() {
        // Arrange: simulate a repository lookup that finds no matching record.
        Long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act and Assert: confirm the service throws a not-found exception.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getRequestById(id));

        assertEquals(404, exception.getStatusCode().value());
        verify(repository).findById(id);
    }

    /* TEST 5: SUCCESSFULLY UPDATING A REQUEST */
    @Test
    void updateRequest_withValidTransition_updatesRequest() {
        // Arrange: simulate an existing OPEN request and configure save().
        Long id = 1L;
        ServiceRequest request = new ServiceRequest(
                "Laptop issue", "Old description.", RequestPriority.MEDIUM);

        when(repository.findById(id)).thenReturn(Optional.of(request));
        when(repository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: update the request and move it from OPEN to IN_PROGRESS.
        ServiceRequest result = service.updateRequest(
                id,
                "Laptop Wi-Fi issue",
                "Unable to connect to the office network.",
                RequestPriority.HIGH,
                RequestStatus.IN_PROGRESS);

        // Assert: confirm the fields and status were updated.
        assertEquals("Laptop Wi-Fi issue", result.getTitle());
        assertEquals(
                "Unable to connect to the office network.",
                result.getDescription());
        assertEquals(RequestPriority.HIGH, result.getPriority());
        assertEquals(RequestStatus.IN_PROGRESS, result.getStatus());

        verify(repository).findById(id);
        verify(repository).save(request);
    }

    /* TEST 6: REJECTING AN INVALID STATUS TRANSITION */
    @Test
    void updateRequest_withInvalidTransition_throwsBadRequest() {
        // Arrange: prepare an OPEN request; OPEN cannot jump directly to CLOSED.
        Long id = 1L;
        ServiceRequest request = new ServiceRequest(
                "Laptop issue", "Laptop won't connect.", RequestPriority.HIGH);

        when(repository.findById(id)).thenReturn(Optional.of(request));

        // Act and Assert: confirm the invalid update is rejected with HTTP 400.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateRequest(
                        id,
                        "Laptop issue",
                        "Laptop still won't connect.",
                        RequestPriority.HIGH,
                        RequestStatus.CLOSED));

        assertEquals(400, exception.getStatusCode().value());

        // Verify that a rejected update is never saved.
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
