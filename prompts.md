# prompts.md

## AI Assistance Disclosure

Primary models/tools used:
- GPT-5.5 Thinking (ChatGPT)
- Cursor AI Agent

The AI tools were used for:
- architecture planning
- endpoint generation
- DTO/entity scaffolding
- validation/business rule implementation
- security configuration
- debugging
- reviewer-style QA
- OpenAPI/Swagger fixes
- test generation
- documentation generation

All generated code was manually reviewed, tested, debugged, and verified before submission.

---

# Main Prompts Used During Development

## 1. Initial Project Architecture Prompt

Model:
- GPT-5.5 Thinking

Main Prompt:
```text
You are a senior Java backend engineer. Build the full TDP 2026 IssueFlow home assignment using Java 21, Spring Boot 3, PostgreSQL, JPA/Hibernate, Spring Security + JWT, Flyway, Docker Compose, validation, tests, and clean layered architecture.

Use the uploaded requirements document as the source of truth. Implement IssueFlow: a RESTful backend API for a lightweight project and ticket management platform with users, projects, tickets, comments, authentication, audit logs, dependencies, attachments, CSV import/export, soft delete, mentions, auto-escalation, and auto-assignment.

Tech stack:
- Java 21
- Spring Boot 3
- PostgreSQL
- Spring Data JPA / Hibernate
- Spring Security + JWT
- Flyway migrations
- Docker Compose
- Maven
- JUnit 5 + Mockito + Spring Boot Test
- Testcontainers if useful
- OpenAPI/Swagger documentation

Architecture:
Use a clean layered structure:

src/main/java/.../
- controller/
- service/
- repository/
- entity/
- dto/
- mapper/
- security/
- scheduler/
- audit/
- exception/
- config/

Core entities:
- User
- Project
- Ticket
- Comment
- AuditLog
- TicketDependency
- Attachment
- Mention

Implement all required APIs from the assignment README/API table. If the README exists in the repo, treat it as the exact API contract.

Functional requirements:

1. User Management
- Register user with username, email, fullName, role, password.
- Roles: ADMIN, DEVELOPER.
- CRUD users.
- Fetch all users.
- Validate role values.

2. Authentication
- JWT-based auth protecting all endpoints.
- POST /auth/login returns signed JWT.
- POST /auth/logout invalidates token using server-side deny-list or clear documented expiry strategy.
- GET /auth/me returns current authenticated user.
- Passwords must be hashed with BCrypt.

3. Project Management
- Create project with name, description, owner userId.
- CRUD projects.
- Fetch all non-deleted projects.
- Soft delete only.
- ADMIN-only deleted project list and restore.

4. Ticket Management
- Create ticket with title, description, status, priority, type, projectId, optional assigneeId, optional dueDate.
- Status enum: TODO, IN_PROGRESS, IN_REVIEW, DONE.
- Priority enum: LOW, MEDIUM, HIGH, CRITICAL.
- Type enum: BUG, FEATURE, TECHNICAL.
- CRUD tickets.
- Fetch tickets by project.
- Soft delete only.
- Do not allow updating DONE tickets.
- Enforce forward-only status transitions:
  TODO -> IN_PROGRESS -> IN_REVIEW -> DONE.
- Reject backward or skipped invalid transitions.
- A ticket cannot move to DONE if unresolved blocker dependencies exist.
- Prevent simultaneous ticket updates using optimistic locking with @Version and return HTTP 409 on conflicts.

5. Comments
- Add comments to tickets.
- Fetch comments by ticket.
- Update comment content.
- Delete comment.
- Prevent simultaneous edits using optimistic locking.
- Parse @username mentions in comments.
- Mention matching must be case-insensitive.
- On comment update, re-evaluate mentions: add new ones and remove deleted ones.
- Include mentionedUsers in comment responses.
- GET /users/{userId}/mentions returns all comments where user was mentioned, newest first.

6. Audit Log
- Persistent append-only audit table.
- Log all state-changing actions:
  user create/update/delete,
  project create/update/delete/restore,
  ticket create/update/delete/restore,
  comment create/update/delete,
  dependency add/remove,
  attachment upload/delete,
  import,
  auto-assignment,
  auto-escalation.
- Include actor, action, entityType, entityId, oldValue, newValue, timestamp.
- For system actions use actor = SYSTEM.
- Provide endpoint to retrieve all logs and filter by fields such as actor, action, entityType, entityId.

7. Ticket Dependencies
- POST /tickets/{ticketId}/dependencies body { "blockedBy": 42 }.
- GET /tickets/{ticketId}/dependencies.
- DELETE /tickets/{ticketId}/dependencies/{blockerId}.
- Both tickets must exist and belong to the same project.
- Prevent duplicate dependencies.
- Prevent self-dependency.
- Ideally prevent dependency cycles.

8. Attachments
- Upload files to tickets using multipart/form-data.
- Max size 10 MB.
- Allowed MIME types:
  image/png,
  image/jpeg,
  application/pdf,
  text/plain.
- Reject all others with clear 400 error.
- Store metadata in DB.
- Store file content either in DB or local storage with clear implementation.
- Add endpoints to upload, download/list, and delete attachments if not specified by README.

9. CSV Export / Import
- GET /tickets/export?projectId={id}
- Return CSV file with:
  id, title, description, status, priority, type, assigneeId.
- POST /tickets/import accepts multipart CSV and target projectId form field.
- Create tickets in bulk.
- Return:
  { "created": number, "failed": number, "errors": [...] }
- CSV must correctly handle commas and quotes inside fields.
- Use a robust CSV library such as Apache Commons CSV.

10. Soft Delete
- Tickets and projects must be soft-deleted, not physically deleted.
- Hide soft-deleted records from normal GET responses.
- ADMIN only:
  GET /tickets/deleted?projectId={id}
  GET /projects/deleted
  POST /tickets/{id}/restore
  POST /projects/{id}/restore

11. Auto-Scheduling Escalation
- Ticket create/update accepts dueDate.
- Scheduled job checks overdue unresolved tickets.
- If overdue and priority below CRITICAL:
  LOW -> MEDIUM -> HIGH -> CRITICAL.
- If already CRITICAL and still overdue, set isOverdue = true.
- Escalation must be idempotent.
- Only applies to tickets with dueDate.
- Manual priority change resets auto-escalation state and clears isOverdue.
- Escalation changes priority/isOverdue only, not status.
- Log all escalations in audit log.

12. Auto Assignment
- When creating ticket without assigneeId:
  select least-loaded DEVELOPER in the same project.
- Workload = count of non-DONE tickets assigned to that user in same project.
- Tie breaker: oldest registered user first.
- If no DEVELOPER linked to project, create ticket unassigned.
- GET /projects/{projectId}/workload returns:
  { userId, username, openTicketCount }
  sorted ascending.
- Log auto-assignment with actor SYSTEM and action AUTO_ASSIGN.
- Do not trigger auto-assignment on ticket update.
- Explicit PATCH assigneeId overrides auto assignment.

Reviewer-impressing requirements:
- Clean DTOs: separate request/response objects from entities.
- Global exception handler using @ControllerAdvice.
- Consistent error response format.
- Validation with jakarta.validation annotations.
- Transaction boundaries using @Transactional in services.
- Optimistic locking with @Version.
- Proper HTTP status codes:
  400 validation/business errors,
  401 unauthenticated,
  403 unauthorized,
  404 not found,
  409 conflict/concurrency,
  201 created,
  204 delete success.
- Flyway migration files for schema.
- Seed data if useful.
- Swagger/OpenAPI enabled.
- Docker Compose for PostgreSQL.
- Good indexes on foreign keys and common filters.
- Avoid business logic in controllers.
- Add meaningful comments only where helpful.
- Use enum types cleanly.
- Make the project easy to run.

Testing:
Create relevant tests for:
- auth login/me
- user CRUD validation
- project CRUD and soft delete
- ticket creation/update
- invalid status transitions
- DONE ticket cannot be updated
- dependency blocks DONE transition
- optimistic locking conflict
- comment mention parsing
- comment update re-evaluates mentions
- auto assignment chooses least-loaded developer
- escalation scheduler behavior
- CSV import/export
- attachment validation
- audit log creation

Documentation files:
1. run.md
Include exact steps:
- install requirements
- start PostgreSQL with Docker Compose
- run Flyway/migrations if needed
- build project
- run app
- run tests
- example curl requests
- default local URLs


2. README.md
Keep or update existing assignment API contract without breaking it.
Add project overview, architecture, features, and decisions.

Implementation strategy:
First inspect the existing skeleton.
Then create a step-by-step plan.
Then implement incrementally:
1. Entities + Flyway migrations
2. Repositories
3. DTOs/mappers
4. Global exceptions
5. Auth/JWT
6. User/project CRUD
7. Ticket rules
8. Comments/mentions
9. Audit log
10. Dependencies
11. Attachments
12. CSV import/export
13. Scheduler escalation
14. Auto assignment/workload
15. Tests
16. Documentation

Important:
Do not skip features.
Do not leave TODOs.
Do not fake tests.
Make sure the app compiles and tests pass.
Use clean, production-like code.
After implementation, give me a summary of files changed, how to run, and any assumptions made.```
```
# Another prompts:

### 1. Business Rules Enforcement Prompt

```text
Implement strict business rules for IssueFlow.

Requirements:
- Ticket status lifecycle:
  TODO -> IN_PROGRESS -> IN_REVIEW -> DONE

Rules:
- backward transitions forbidden
- DONE immutable
- invalid transitions return 400

Dependencies:
- same project only
- no self dependency
- no duplicate dependency
- no cyclic dependency
- blocked tickets cannot become DONE

Implement proper validation and meaningful error responses.
```
### 2. JWT + Swagger Security Prompt
```text
Configure JWT authentication and Swagger/OpenAPI integration.

Requirements:
- JWT bearer auth
- Swagger Authorize button
- protected endpoints show lock icons
- login endpoint remains public
- revoked token deny-list logout
- Spring Security configuration for Swagger paths
- OpenAPI security scheme configuration

Fix all Swagger schema/security issues and ensure /v3/api-docs is valid.
```
### 3. Final OpenAPI/Swagger Debugging Prompt
```text
Fix Swagger/OpenAPI configuration completely.

Requirements:
- visible Authorize button
- JWT bearer auth support
- no unresolved schema references
- protected endpoints show locks
- login endpoint public
- valid /v3/api-docs output
- no duplicate OpenAPI beans
- compatible with Spring Boot 3.4 + springdoc-openapi 2.8.5

Add tests verifying bearerAuth appears in OpenAPI JSON.
```

### 4. Final Submission Readiness Audit
```text
Perform a final pre-submission audit.

Verify:
- application builds
- tests pass
- Docker works
- Flyway migrations work
- Swagger works
- JWT auth works
- all endpoints function
- README accuracy
- prompts.md completeness

Then provide:
- remaining risks
- reviewer concerns
- confidence score
- exact final submission steps
```