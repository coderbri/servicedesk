
package com.coderbri.servicedesk.controller;

import com.coderbri.servicedesk.dto.CreateServiceRequestRequest;
import com.coderbri.servicedesk.dto.UpdateServiceRequestRequest;
import com.coderbri.servicedesk.entity.RequestPriority;
import com.coderbri.servicedesk.entity.RequestStatus;
import com.coderbri.servicedesk.entity.ServiceRequest;
import com.coderbri.servicedesk.service.ServiceRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @file: controller/ServiceRequestControllerTest.java
 * @description:
 * Controller tests ask: “Does the HTTP API behave correctly?”
 */
@WebMvcTest(ServiceRequestController.class)
class ServiceRequestControllerTest {

    /** = THE CONTROLLER TEST CLASS =
     * @WebMvcTest(....class) asks Spring to configure the MVC testing infrastructure
     *  for this controller, rather than starting the entire application with its db.
     * MockMvc allows request simulation such as POST /api/requests and inspects their responses.
     * @MockitoBean provides a mock ServiceRequestService, so the tests can control what the
     *  controller receives from the service.
     */
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceRequestService service;

    /* TEST 1: POST RETURNS 201 CREATED */
    @Test
    void createRequest_returnsCreated() throws Exception {
        // Arrange: configure the mock service to return a saved request.
        ServiceRequest request = createRequest(
                1L, "Laptop issue", "Cannot connect to Wi-Fi",
                RequestPriority.HIGH, RequestStatus.OPEN);

        when(service.createRequest(
                eq("Laptop issue"),
                eq("Cannot connect to Wi-Fi"),
                eq(RequestPriority.HIGH)))
                .thenReturn(request);

        String json = """
            {
                "title": "Laptop issue",
                "description": "Cannot connect to Wi-Fi",
                "priority": "HIGH"
            }
            """;

        // Act and Assert: send POST and verify the HTTP response and JSON.
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Laptop issue"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(service).createRequest(
                "Laptop issue",
                "Cannot connect to Wi-Fi",
                RequestPriority.HIGH);
    }

    /* TEST 2: INVALID INPUT RETURNS 400 BAD REQUEST */
    @Test
    void createRequest_withBlankTitle_returnsBadRequest() throws Exception {
        // Arrange: prepare JSON that violates the title's @NotBlank constraint.
        String json = """
            {
                "title": "",
                "description": "Cannot connect to Wi-Fi",
                "priority": "HIGH"
            }
            """;

        // Act and Assert: send the request and confirm validation rejects it.
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "POST rejects invalid request body {index}")
    @MethodSource("invalidCreateRequests")
    void createRequest_withInvalidFields_returnsBadRequest(String json) throws Exception {
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    static Stream<String> invalidCreateRequests() {
        return Stream.of(
                """
                    {"title":"Valid title","description":"   ","priority":"HIGH"}
                    """,
                """
                    {"title":"%s","description":"Valid description","priority":"HIGH"}
                    """.formatted("x".repeat(151)),
                """
                    {"title":"Valid title","description":"%s","priority":"HIGH"}
                    """.formatted("x".repeat(2001)),
                """
                    {"title":"Valid title","description":"Valid description"}
                    """,
                """
                    {"title":"Valid title","description":"Valid description","priority":"URGENT"}
                    """
        );
    }

    /* TEST 3: GET ALL RETURNS A JSON ARRAY */
    @Test
    void getAllRequests_returnsOkAndList() throws Exception {
        // Arrange: configure the service to return two sample requests.
        ServiceRequest first = createRequest(
                1L, "Laptop issue", "Cannot connect to Wi-Fi",
                RequestPriority.HIGH, RequestStatus.OPEN);

        ServiceRequest second = createRequest(
                2L, "Monitor issue", "Screen is blank",
                RequestPriority.MEDIUM, RequestStatus.OPEN);

        when(service.getAllRequests()).thenReturn(List.of(first, second));

        // Act and Assert: request the collection and inspect its JSON.
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Laptop issue"))
                .andExpect(jsonPath("$[1].title").value("Monitor issue"));

        verify(service).getAllRequests();
    }

    /* TEST 4: GET BY ID SUCCEEDS */
    @Test
    void getRequestById_whenFound_returnsOk() throws Exception {
        // Arrange: configure the service to return the request for ID 1.
        ServiceRequest request = createRequest(
                1L, "Laptop issue", "Cannot connect to Wi-Fi",
                RequestPriority.HIGH, RequestStatus.OPEN);

        when(service.getRequestById(1L)).thenReturn(request);

        // Act and Assert: request ID 1 and check the returned JSON.
        mockMvc.perform(get("/api/requests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Laptop issue"));

        verify(service).getRequestById(1L);
    }

    /* TEST 5: GET BY ID RETURNS 404 */
    @Test
    void getRequestById_whenMissing_returnsNotFound() throws Exception {
        // Arrange: simulate the service reporting that ID 999 does not exist.
        when(service.getRequestById(999L)).thenThrow(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Service request not found: 999"));

        // Act and Assert: request the missing ID and expect HTTP 404.
        mockMvc.perform(get("/api/requests/999"))
                .andExpect(status().isNotFound());

        verify(service).getRequestById(999L);
    }

    /* TEST 6: PUT RETURNS 200 OK */
    @Test
    void updateRequest_returnsOk() throws Exception {
        // Arrange: configure the service to return the updated request.
        ServiceRequest request = createRequest(
                1L, "Laptop Wi-Fi issue", "Cannot connect to office network",
                RequestPriority.HIGH, RequestStatus.IN_PROGRESS);

        when(service.updateRequest(
                eq(1L),
                eq("Laptop Wi-Fi issue"),
                eq("Cannot connect to office network"),
                eq(RequestPriority.HIGH),
                eq(RequestStatus.IN_PROGRESS)))
                .thenReturn(request);

        String json = """
            {
                "title": "Laptop Wi-Fi issue",
                "description": "Cannot connect to office network",
                "priority": "HIGH",
                "status": "IN_PROGRESS"
            }
            """;

        // Act and Assert: send PUT and check the response.
        mockMvc.perform(put("/api/requests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(service).updateRequest(
                1L,
                "Laptop Wi-Fi issue",
                "Cannot connect to office network",
                RequestPriority.HIGH,
                RequestStatus.IN_PROGRESS);
    }

    @ParameterizedTest(name = "PUT rejects invalid request body {index}")
    @MethodSource("invalidUpdateRequests")
    void updateRequest_withInvalidFields_returnsBadRequest(String json) throws Exception {
        mockMvc.perform(put("/api/requests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    static Stream<String> invalidUpdateRequests() {
        return Stream.of(
                """
                    {"title":"  ","description":"Valid description","priority":"HIGH","status":"OPEN"}
                    """,
                """
                    {"title":"Valid title","description":"  ","priority":"HIGH","status":"OPEN"}
                    """,
                """
                    {"title":"%s","description":"Valid description","priority":"HIGH","status":"OPEN"}
                    """.formatted("x".repeat(151)),
                """
                    {"title":"Valid title","description":"%s","priority":"HIGH","status":"OPEN"}
                    """.formatted("x".repeat(2001)),
                """
                    {"title":"Valid title","description":"Valid description","status":"OPEN"}
                    """,
                """
                    {"title":"Valid title","description":"Valid description","priority":"HIGH"}
                    """,
                """
                    {"title":"Valid title","description":"Valid description","priority":"URGENT","status":"OPEN"}
                    """,
                """
                    {"title":"Valid title","description":"Valid description","priority":"HIGH","status":"UNKNOWN"}
                    """
        );
    }

    /* HELPER METHOD: PREPARING TEST REQUESTS */
    private ServiceRequest createRequest(
            Long id,
            String title,
            String description,
            RequestPriority priority,
            RequestStatus status) {

        ServiceRequest request = new ServiceRequest(
                title, description, priority);

        // Set persistence-managed fields to simulate a saved entity:
        //  Assign the ID, status, and timestamp as if the entity
        //  had been returned from the database.
        try {
            var idField = ServiceRequest.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(request, id);

            var statusField = ServiceRequest.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(request, status);

            var createdAtField =
                    ServiceRequest.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(request, LocalDateTime.now());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not prepare test service request", exception);
        }

        return request;
    }
}
