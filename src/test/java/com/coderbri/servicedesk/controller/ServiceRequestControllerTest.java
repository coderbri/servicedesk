package com.coderbri.servicedesk.controller;

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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @file: test/.../ServiceRequestControllerTest.java
 * @description: Controller tests exercise HTTP status, JSON mapping, and request validation.
 */
@WebMvcTest(ServiceRequestController.class)
class ServiceRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceRequestService service;

    @Test
    void createRequest_returnsCreated() throws Exception {
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

    @Test
    void createRequest_withBlankTitle_returnsBadRequest() throws Exception {
        String json = """
            {
                "title": "",
                "description": "Cannot connect to Wi-Fi",
                "priority": "HIGH"
            }
            """;

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

    @Test
    void getAllRequests_returnsOkAndList() throws Exception {
        ServiceRequest first = createRequest(
                1L, "Laptop issue", "Cannot connect to Wi-Fi",
                RequestPriority.HIGH, RequestStatus.OPEN);

        ServiceRequest second = createRequest(
                2L, "Monitor issue", "Screen is blank",
                RequestPriority.MEDIUM, RequestStatus.OPEN);

        when(service.getAllRequests()).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Laptop issue"))
                .andExpect(jsonPath("$[1].title").value("Monitor issue"));

        verify(service).getAllRequests();
    }

    @Test
    void getRequestById_whenFound_returnsOk() throws Exception {
        ServiceRequest request = createRequest(
                1L, "Laptop issue", "Cannot connect to Wi-Fi",
                RequestPriority.HIGH, RequestStatus.OPEN);

        when(service.getRequestById(1L)).thenReturn(request);

        mockMvc.perform(get("/api/requests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Laptop issue"));

        verify(service).getRequestById(1L);
    }

    @Test
    void getRequestById_whenMissing_returnsNotFound() throws Exception {
        when(service.getRequestById(999L)).thenThrow(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Service request not found: 999"));

        mockMvc.perform(get("/api/requests/999"))
                .andExpect(status().isNotFound());

        verify(service).getRequestById(999L);
    }

    @Test
    void updateRequest_returnsOk() throws Exception {
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

    private ServiceRequest createRequest(
            Long id,
            String title,
            String description,
            RequestPriority priority,
            RequestStatus status) {

        ServiceRequest request = mock(ServiceRequest.class);
        when(request.getId()).thenReturn(id);
        when(request.getTitle()).thenReturn(title);
        when(request.getDescription()).thenReturn(description);
        when(request.getPriority()).thenReturn(priority);
        when(request.getStatus()).thenReturn(status);
        when(request.getCreatedAt()).thenReturn(LocalDateTime.now());
        return request;
    }
}
