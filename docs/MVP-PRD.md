# ServiceDesk MVP: Product Requirements and Roadmap

**Status:** Draft proposal  
**Product:** ServiceDesk  
**Purpose:** Define a small, demonstrable first release before feature implementation begins.

## Product summary

ServiceDesk will help a small support team record service requests and track them from submission through resolution. The MVP should demonstrate a complete, reliable ticket lifecycle backed by Oracle Free, with a simple web interface and a documented API.

## Problem and users

Support requests handled through informal messages are easy to lose and difficult to track. The MVP gives requesters a consistent way to submit an issue and gives support staff one place to review and update it.

- **Requester:** submits a request and checks its status.
- **Support agent:** reviews requests, sets priority, assigns ownership, and updates status.

## MVP goals

- A requester can submit a valid service request.
- An agent can find, inspect, prioritize, assign, and update requests.
- Each request has a clear, auditable lifecycle and persists in Oracle.
- Invalid input and missing records produce clear responses.
- The project can be run locally using the Gradle wrapper and documented configuration.

## Proposed scope and assumptions

This draft assumes one small support team, a single application deployment, and no external integrations. Start with a simple web experience backed by REST endpoints. Keep the first release focused on the ticket workflow.

### Included

- Create and view service requests.
- List and filter requests by status and priority.
- Update priority, assignee, and status.
- Add a resolution note when closing a request.
- Validate input and return useful error responses.
- Persist requests with timestamps in Oracle Free.
- Document local setup, configuration, and API usage.

### Deferred

- User accounts, roles, and authentication.
- Email notifications, attachments, comments or conversation threads.
- SLA timers, escalation rules, analytics dashboards, and audit history.
- External identity, chat, or ticketing integrations.
- Multi-team queues and configurable workflows.

Because authentication is deferred, the initial release is for local development or a trusted demo environment only. Do not expose it publicly with real requester data until access control is added.

## Functional requirements

1. **Submit request:** Accept a subject, description, requester name, and requester email. Set a generated ID, `OPEN` status, `NORMAL` priority, and creation/update timestamps.
2. **Browse requests:** Return requests newest first. Support optional status and priority filters.
3. **View request:** Return one request by ID or a clear not-found response.
4. **Manage request:** Allow an agent to set priority, assignee, and status. Reject unsupported values and invalid transitions.
5. **Resolve request:** A request can be closed with a resolution note. Reopening is allowed by changing status back to `OPEN` or `IN_PROGRESS`.
6. **Validation:** Reject blank or overlong subject/description, malformed email, and missing required fields with field-level feedback.
7. **Persistence:** Keep requests across application restarts. Database schema changes must be deliberate; replace automatic schema updates with a migration approach before broader use.

## Proposed data model

A `Ticket` record should include: ID, subject, description, requester name, requester email, status, priority, assignee (optional), resolution note (optional), created timestamp, and updated timestamp.

Suggested statuses: `OPEN`, `IN_PROGRESS`, `CLOSED`. Suggested priorities: `LOW`, `NORMAL`, `HIGH`, `URGENT`. Keep these values fixed for the MVP.

## Acceptance criteria

- A valid request can be created and read back after restarting the application.
- Required-field and email validation failures are understandable to a caller.
- Listing supports status and priority filters and has a stable newest-first order.
- A missing ticket returns HTTP 404; invalid input returns HTTP 400; successful creation returns HTTP 201.
- Agents can update supported fields and close a request with a resolution note.
- The app starts with Oracle Free configured and without putting credentials in source control.
- The root page/API behavior is intentional and documented; the current bare `/` 404 is not mistaken for an application failure.

## Roadmap

### Phase 1 — Foundation

- Establish project conventions, configuration profiles, and a health check.
- Define ticket/status/priority model and database migration strategy.
- Add automated tests for validation and persistence.

### Phase 2 — Ticket API

- Implement create, list/filter, and get-by-ID endpoints.
- Implement ticket updates, status transitions, and resolution notes.
- Add consistent validation and error responses; document endpoints.

### Phase 3 — Usable MVP experience

- Add a simple page to submit requests and browse the queue.
- Add ticket detail and agent controls for assignment, priority, and status.
- Verify the workflow end to end against Oracle Free.

### Phase 4 — Release readiness

- Review configuration and secret handling; add authentication before any public deployment.
- Add operational health checks, setup instructions, and a small demo dataset or walkthrough.
- Fix usability gaps found during a complete requester-to-resolution walkthrough.

## Success measures

- A new request can be submitted and found in the queue in under one minute.
- An agent can move a request from open to closed without editing the database directly.
- Core ticket workflows pass repeatable automated tests.

## Decisions to confirm before implementation

- Should requesters be able to see only their own requests, or is this a shared internal queue?
- Should authentication be part of the first MVP, or should the first release remain a local/trusted demo?
- Are the proposed statuses and priorities sufficient for the intended demonstration?
