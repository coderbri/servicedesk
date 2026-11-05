# Learning notes: Understanding the service request tests

These notes explain the two Phase 5 automated test files:

- `src/test/java/com/coderbri/servicedesk/service/ServiceRequestServiceTest.java`
- `src/test/java/com/coderbri/servicedesk/controller/ServiceRequestControllerTest.java`

The examples below describe the current tests. They are learning notes, not a test run report.

## Problem

When tests are new, annotations, mocks, and assertions can make a test file look like a second application. The tests have a narrower job: check that one part of the application responds correctly to prepared inputs.

## Root cause: the tests isolate different layers

The service tests call the real `ServiceRequestService` and replace its repository dependency with a Mockito mock. The controller tests run the Spring MVC web layer and replace the service dependency with a mock. This lets each test control the next layer's response and focus on the behavior of the layer under test.

These service and controller tests do **not** verify real Oracle persistence. The repository is mocked in the service tests, and the controller test uses a mocked service with a web-focused Spring test setup. Database integration is a separate kind of test that exercises the actual database connection and persistence configuration.

The manual endpoint walkthrough in [02-create-service-request-api.md](02-create-request-implementation-walkthrough.md) and the Oracle/Postman verification in [05-ORACLE-QUOTA-AND-POSTMAN.md](../05-ORACLE-QUOTA-AND-POSTMAN.md) serve a different purpose: they explain how a person can send requests and inspect results. The automated tests below preserve key input and lifecycle rules as repeatable checks, but they do not replace those hands-on instructions or prove database persistence.

The project uses Spring Boot `4.1.1` and the feature-specific test starters in `build.gradle`: `spring-boot-starter-data-jpa-test`, `spring-boot-starter-validation-test`, and `spring-boot-starter-webmvc-test`. The controller imports `WebMvcTest` from `org.springframework.boot.webmvc.test.autoconfigure` and `MockitoBean` from `org.springframework.test.context.bean.override.mockito`, which are the packages used in this project.

## Service test: `ServiceRequestServiceTest.java`

### Package and imports

The package declaration, `com.coderbri.servicedesk.service`, places this test in the service package alongside the class it tests. Java uses packages to group related classes and distinguish classes with the same name.

The imports name the tools and application types used in the file:

| Import group | Purpose |
| --- | --- |
| `RequestPriority`, `RequestStatus`, `ServiceRequest` | The request data and enum values the tests prepare and inspect. |
| `ServiceRequestRepository` | The service's persistence dependency; it is mocked here. |
| `@BeforeEach`, `@Test`, `@ExtendWith` | JUnit annotations for per-test setup, test methods, and an extension. |
| `@Mock`, `MockitoExtension` | Mockito support for creating and initializing the repository mock. |
| `ResponseStatusException` | The exception type used for expected service errors. |
| `List`, `Optional` | Java collection and “value may be absent” types used in repository results. |
| `assertEquals`, `assertSame`, `assertThrows` | JUnit checks for values, object identity, and expected exceptions. |
| `when`, `thenReturn`, `thenAnswer`, `verify`, `never`, `any` | Mockito tools for setting up and checking mock interactions. |

The static imports allow calls such as `assertEquals(...)` and `when(...)` without writing the full class name each time.

### JUnit and Mockito setup

`@ExtendWith(MockitoExtension.class)` connects Mockito to JUnit 5 for this test class. It initializes annotated mocks for each test. `@Mock` asks Mockito to create a substitute `ServiceRequestRepository`; calls to it do not go to Oracle.

`@BeforeEach` makes JUnit call `setUp()` before every test. The method creates a fresh real `ServiceRequestService`, passing in the mock repository. Each service test therefore gets a service with a controlled dependency.

### Arrange, Act, Assert

Most tests follow **Arrange, Act, Assert (AAA)**:

1. **Arrange** prepares input data and configures mock responses.
2. **Act** calls the real method under test.
3. **Assert** checks the returned value, exception, or interaction.

Some tests combine Act and Assert because the most useful outcome is an exception or an HTTP response.

### Mock setup: `when`, `thenReturn`, and `thenAnswer`

`when(repository.findAll()).thenReturn(List.of(first, second))` tells the mock what to return when that method is called. This makes the service receive a predictable list without reading a database.

`Optional.of(request)` represents a repository lookup that found a request. `Optional.empty()` represents a lookup with no matching request. `Optional` makes the possibility of “found” versus “not found” explicit rather than using a request value that might be `null`.

For `save`, the tests use:

```java
when(repository.save(any(ServiceRequest.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
```

`any(ServiceRequest.class)` matches a save call with a `ServiceRequest`. `thenAnswer` runs an answer when the mock is called. Here it returns argument zero—the request passed to `save`—so the service receives that same object back. This is useful when the test needs a simple save response but does not need a database-generated ID.

### Assertions and mock verification

- `assertEquals(expected, actual)` checks that values are equal, such as a title, status, list size, or HTTP code.
- `assertSame(expectedObject, actualObject)` checks that both references point to the exact same object. The retrieval test uses it to show that the service returned the request supplied by the mock.
- `assertThrows(ExceptionType.class, action)` runs an action and checks that it throws the expected kind of exception. The test can then inspect that exception, such as its status code.
- `verify(repository).findById(id)` checks that the service called the repository method with that ID.
- `verify(repository, never()).save(...)` checks that the service did not try to save after rejecting an invalid transition.

Assertions check outcomes; Mockito verification checks whether a dependency was called (or deliberately not called).

### What each service test covers

| Test method | Scenario and check |
| --- | --- |
| `createRequest_savesRequest` | Creates a request; checks its fields and initial `OPEN` status, then verifies `save` was called. |
| `getAllRequests_returnsAllRequests` | Configures two results; checks the returned list size and order, then verifies `findAll`. |
| `getRequestById_whenRequestExists_returnsRequest` | Returns `Optional.of(request)`; checks the exact object is returned and verifies the lookup. |
| `getRequestById_whenRequestDoesNotExist_throwsNotFound` | Returns `Optional.empty()`; checks for a not-found exception with status 404 and verifies the lookup. |
| `updateRequest_withValidTransition_updatesRequest` | Supplies an existing request; checks updated fields and status, and verifies lookup and save. |
| `updateRequest_withInvalidTransition_throwsBadRequest` | Attempts an invalid `OPEN` to `CLOSED` transition; checks for status 400 and verifies no save occurred. |
| `updateRequest_withAllowedTransition_savesRequest` | Parameterized checks that a request can stay in each status or advance exactly one lifecycle step. |
| `updateRequest_withDisallowedTransition_doesNotSave` | Parameterized checks all backward, skipped, and out-of-`CLOSED` transitions return 400 without saving or changing the status. |
| `updateRequest_whenRequestDoesNotExist_throwsNotFoundWithoutSaving` | Checks that updating a missing ID returns 404 and does not save. |

Negative tests matter because they check how the code handles invalid or missing data, not only successful paths.

## Controller test: `ServiceRequestControllerTest.java`

### Package and imports

The package declaration, `com.coderbri.servicedesk.controller`, keeps the test in the controller package. Imports include request DTOs, entity enums and entity type, and the service type that the controller calls.

The test-specific imports have these roles:

| Import group | Purpose |
| --- | --- |
| `@Test` | Marks methods that JUnit should run as tests. |
| `@Autowired` | Asks Spring to provide the configured `MockMvc` instance. |
| `@WebMvcTest` | Starts a focused Spring MVC test setup for the selected controller. |
| `@MockitoBean` | Adds a Mockito mock in Spring's application context for the service dependency. |
| `MockMvc`, request builders, result matchers | Build simulated HTTP requests and inspect responses. |
| `MediaType` | Identifies JSON request content. |
| `LocalDateTime`, `List` | Set up sample entity data and collections. |
| Mockito matchers and methods | Configure the service mock and verify calls to it. |

### Spring MVC test setup: `@WebMvcTest`, `@MockitoBean`, and `MockMvc`

`@WebMvcTest(ServiceRequestController.class)` asks Spring Boot to configure the MVC testing infrastructure for this controller. It focuses on web behavior instead of starting the full application and its database configuration.

`@Autowired MockMvc` is Spring's tool for sending simulated HTTP requests through the MVC layer in a test. It does not open a network port. `@MockitoBean ServiceRequestService service` supplies a mock service to the Spring test context, so a controller test can decide what the service returns or throws.

### HTTP requests, status, and JSONPath

`mockMvc.perform(...)` sends a simulated request, such as `POST /api/requests`, `GET /api/requests`, or `PUT /api/requests/1`. For JSON input, `.contentType(MediaType.APPLICATION_JSON)` identifies the body format and `.content(json)` supplies the body.

The chained `.andExpect(...)` calls check the response:

- `status().isCreated()`, `isOk()`, `isBadRequest()`, and `isNotFound()` check the HTTP status code (201, 200, 400, and 404 respectively).
- `jsonPath("$.title").value("Laptop issue")` checks the `title` property at the JSON object's root. `$` means the root; `.title` selects that property.
- `jsonPath("$.length()").value(2)` checks the number of items in a JSON array. `$[0].title` and `$[1].title` select the title in the first and second array entries.

These checks verify what the API returns at the MVC boundary, including JSON serialization and the status chosen by the controller/framework.

### Mock configuration and verification

`when(service.createRequest(eq("Laptop issue"), ...)).thenReturn(request)` configures the service mock to return the prepared request when called with those arguments. `eq(...)` is an argument matcher that compares an argument with the expected value. The update test uses the same pattern for several arguments.

`when(service.getRequestById(999L)).thenThrow(...)` configures the missing-request scenario so the controller's error handling can be checked as an HTTP 404 response.

`verify(service).createRequest(...)` checks the controller passed the expected values to the service. In the blank-title validation test, `verifyNoInteractions(service)` checks that validation rejected the request before the controller called the service. This is a negative test: it checks both the 400 response and the absence of a downstream call.

### What each controller test covers

| Test method | Scenario and check |
| --- | --- |
| `createRequest_returnsCreated` | Sends valid JSON; checks 201 and selected response fields, then verifies service arguments. |
| `createRequest_withBlankTitle_returnsBadRequest` | Sends a blank title; checks 400 and no service interactions. |
| `createRequest_withInvalidFields_returnsBadRequest` | Parameterized checks blank descriptions, overlong fields, missing priority, and an invalid priority value; each returns 400 before the service is called. |
| `getAllRequests_returnsOkAndList` | Configures two service results; checks 200, array length, and both titles. |
| `getRequestById_whenFound_returnsOk` | Configures a found request; checks 200 and selected JSON fields. |
| `getRequestById_whenMissing_returnsNotFound` | Configures a not-found exception; checks the HTTP 404 response. |
| `updateRequest_returnsOk` | Sends valid update JSON; checks 200 and updated response fields, then verifies service arguments. |
| `updateRequest_withInvalidFields_returnsBadRequest` | Parameterized checks blank/overlong fields, missing priority or status, and invalid enum values; each returns 400 before the service is called. |

## The `createRequest()` helper and reflection caveat

The private `createRequest(...)` helper builds a `ServiceRequest` with predictable values for reuse across controller tests. The entity's ID, status, and creation time are persistence-managed fields, so the helper sets them to simulate an entity that has already been saved.

It uses Java reflection (`getDeclaredField`, `setAccessible(true)`, and `Field.set`) to assign those fields directly. Reflection can access fields outside the usual public getter/setter path, which is why it works for fields without public setters. This couples the helper to the entity's private field names: renaming a field can break the helper. If a field cannot be found or set, the helper wraps that failure in `IllegalStateException` with a descriptive message. This helper prepares test data; it does not persist anything.

## Key takeaway

The service tests check business logic with a mocked repository. The controller tests check HTTP behavior with Spring MVC and a mocked service. Their mocks keep these tests independent of Oracle, so a successful result from either kind would not by itself prove real database persistence. Database integration must be checked separately.
