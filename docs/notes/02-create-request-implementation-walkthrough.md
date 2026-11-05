# Milestone: ServiceDesk API: create a service request and save it in Oracle

**Status:** `POST /api/requests` creates and persists a request; the endpoint responds with `201 Created` and a JSON representation.

### Goal

A client sends a request such as:

```json
{
  "title": "Laptop won't connect to Wi-Fi",
  "description": "The laptop cannot connect to the office network.",
  "priority": "HIGH"
}
```

The API validates the input, saves it through the repository, and returns the generated ID, initial status, and creation time along with the submitted fields.

### Package structure and files

The Java package root is `com.coderbri.servicedesk`. Packages group code by responsibility, making it easier to find and change one layer at a time.

| Package | Purpose | Files in this milestone |
| --- | --- | --- |
| `entity` | JPA model mapped to stored data; domain enums | `ServiceRequest`, `RequestStatus`, `RequestPriority` |
| `repository` | Database access abstraction | `ServiceRequestRepository` |
| `service` | Application operation and coordination | `ServiceRequestService` |
| `dto` | Input/output shapes exchanged with clients | `CreateServiceRequestRequest`, `ServiceRequestResponse` |
| `controller` | HTTP route and request/response handling | `ServiceRequestController` |

### How the request flows

1. `ServiceRequestController` maps `POST /api/requests` with `@RestController`, `@RequestMapping`, and `@PostMapping`. `@RequestBody` converts JSON into the input DTO, and `@Valid` runs its validation rules.
2. `CreateServiceRequestRequest` uses `@NotBlank` and `@Size` for title and description, and `@NotNull` for priority. The size limits match the entity columns. `RequestPriority` and `RequestStatus` restrict values to known enum choices.
3. The controller passes validated fields to `ServiceRequestService`. `@Service` registers it as a Spring-managed service; constructor injection supplies `ServiceRequestRepository`.
4. The service constructs a `ServiceRequest` and calls `repository.save`. The repository extends `JpaRepository<ServiceRequest, Long>`, which provides common persistence operations without handwritten SQL.
5. `ServiceRequest` is marked `@Entity` and maps to `service_requests` with `@Table`. `@Id` marks the primary key; `@GeneratedValue` and `@SequenceGenerator` configure sequence-based ID generation. `@Column` configures column constraints, and `@Enumerated(EnumType.STRING)` stores enum names as readable strings.
6. The entity constructor sets status to `OPEN`; `@PrePersist` supplies `OPEN` if still absent and sets `createdAt` before the first insert. The saved entity is copied into `ServiceRequestResponse`, which exposes the response fields without returning the entity itself. `ResponseEntity.status(HttpStatus.CREATED)` sets the HTTP status to 201.

### Reuse this setup in another project

1. Start with the domain object and its fields. Decide what is required, field lengths, enum values, and how IDs and timestamps are created.
2. Create the entity and enums, then create a repository extending `JpaRepository<YourEntity, YourIdType>`.
3. Define input and output DTOs separately. Put request validation constraints on the input DTO.
4. Add a service that accepts the repository and implements the operation, then add a controller that maps the HTTP route, validates input, calls the service, and chooses the response status.
5. Keep package names aligned with responsibilities and make sure component/entity packages are under the Spring Boot application package so component and entity scanning can find them.

### Key takeaway

Each layer has one job: the controller handles HTTP, DTOs define the API contract, the service coordinates the use case, the repository handles persistence, and the entity maps domain data to the database. Keeping those jobs separate makes the pattern easier to reuse and test.
