# SaaS Platform — Overall Project Specification

## 1. Project Overview

`saas-platform` is a multi-tenant B2B SaaS platform designed as a portfolio-grade full-stack system.

The project is being built as a realistic SaaS backend + frontend rather than as a collection of isolated demos.

Repository:

`github.com/Parth152-create/saas-platform`

Local repository:

`/Users/parth/IdeaProjects/saas-platform`

Primary goals:

- multi-tenant architecture
- secure authentication
- RBAC
- SSO
- audit logging
- tenant onboarding
- teammate invitations
- Stripe billing
- maintainable frontend
- production-oriented engineering practices
- strong automated verification

---

# 2. Technology Stack

## Backend

- Spring Boot 4.1.1
- Java 25
- Hibernate 7.4.5
- PostgreSQL 16
- Redis 7
- Spring Security
- JWT / Nimbus
- Google OIDC
- Flyway
- Stripe Java SDK
- SpringDoc/OpenAPI
- JUnit
- Mockito

Backend:

```text
http://localhost:8090
```

PostgreSQL:

```text
localhost:5434
```

Redis:

```text
localhost:6380
```

Base package:

```text
com.yourco.saas
```

Architecture:

```text
package-by-feature
```

## Frontend

- React 19
- TypeScript
- Vite 8

Development frontend:

```text
http://localhost:5173
```

---

# 3. System Architecture

High-level:

```text
                    ┌─────────────────────┐
                    │      Frontend       │
                    │ React + TypeScript  │
                    │       + Vite        │
                    └──────────┬──────────┘
                               │
                               │ HTTP / JSON
                               ▼
                    ┌─────────────────────┐
                    │    Spring Boot      │
                    │      Backend        │
                    └──────────┬──────────┘
                               │
             ┌─────────────────┼─────────────────┐
             │                 │                 │
             ▼                 ▼                 ▼
       PostgreSQL            Redis             Stripe
             │
             ▼
      Tenant Schemas
```

The backend owns:

- authentication
- authorization
- tenant resolution
- database access
- Stripe interaction
- business rules
- security

The frontend owns:

- presentation
- UX
- client-side state
- routing
- API consumption
- user interaction

The frontend must never bypass backend security.

---

# 4. Source of Truth

This document describes the overall project architecture and direction.

The actual repository remains the implementation source of truth.

When modifying the project:

1. inspect existing code
2. inspect existing tests
3. inspect migrations
4. inspect DTOs/controllers
5. inspect configuration
6. verify assumptions
7. make the smallest appropriate change

Do not blindly implement from this document if the repository has evolved.

---

# 5. Multi-Tenancy

The application uses **schema-per-tenant PostgreSQL multi-tenancy**.

Core pieces:

- `TenantContext`
- tenant identifier resolver
- tenant connection provider
- connection schema switching
- tenant provisioning
- tenant registry
- per-tenant Flyway migrations

Conceptual request flow:

```text
HTTP Request
    ↓
Authentication
    ↓
Tenant Resolution
    ↓
TenantContext
    ↓
Hibernate Connection
    ↓
Tenant PostgreSQL Schema
```

The public tenant registry lives separately from tenant-specific data.

`TenantRegistryService` uses plain JDBC/JdbcTemplate against `public.tenant_registry` because tenant resolution can happen before a tenant schema exists.

Do not use JPA for the public tenant registry when doing so would create a tenant-resolution chicken-and-egg problem.

---

# 6. Tenant Naming / Validation

Tenant IDs and schema names are validated by the backend.

Schema names follow:

```text
^[a-z][a-z0-9_]{0,62}$
```

Tenant IDs follow the signup-specific validation implemented in the backend.

Never trust client-supplied schema names.

Never allow a frontend request to directly select a PostgreSQL schema.

---

# 7. Authentication

Supported authentication flows:

```text
POST /api/auth/signup
POST /api/auth/login
POST /api/auth/google
POST /api/auth/refresh
POST /api/auth/accept-invite
```

Authentication uses JWTs with RS256/Nimbus.

Refresh tokens rotate through Redis.

The frontend must consume the real backend authentication contract.

---

# 8. Signup

Signup creates:

1. tenant
2. tenant registry entry
3. first user
4. first user with SUPER_ADMIN role
5. authenticated token response

Conceptual request:

```json
{
  "tenantId": "acme",
  "email": "admin@example.com",
  "password": "password"
}
```

Password minimum is 8 characters.

Tenant IDs have backend-defined validation.

---

# 9. Login

Login requires:

```text
tenantId
email
password
```

Authentication failures are intentionally handled without unnecessarily revealing which part failed.

Disabled and inactive/invited users must not be treated as authenticated.

---

# 10. Google OIDC

Google login is invite-only.

Conceptual flow:

```text
ADMIN creates INVITED user
          ↓
User attempts Google login
          ↓
Backend verifies Google token
          ↓
Email matches invited user
          ↓
User activated
          ↓
JWT tokens issued
```

Google identity mismatches must be rejected.

Unverified/invalid Google tokens must be rejected.

Do not remove invite-only behavior merely to make Google login easier.

---

# 11. Refresh Tokens

Refresh tokens rotate.

The old refresh token is single-use.

Conceptual flow:

```text
Access Token expires
        ↓
Frontend calls /refresh
        ↓
Backend validates refresh token
        ↓
Old token invalidated
        ↓
New access + refresh tokens
```

The frontend must avoid infinite refresh loops.

If refresh fails, authentication should be cleared and the user returned to login.

---

# 12. RBAC

Role hierarchy:

```text
SUPER_ADMIN
      ↓
ADMIN
      ↓
MANAGER
      ↓
USER
```

The hierarchy is implemented in Spring Security.

Frontend role checks are for UX.

Backend role checks are authoritative.

Never rely on the frontend to protect an endpoint.

---

# 13. Role Assignment Security

There was a role escalation vulnerability in the teammate invitation flow.

The current intended rules are:

```text
SUPER_ADMIN → SUPER_ADMIN, ADMIN, MANAGER, USER
ADMIN       → ADMIN, MANAGER, USER
MANAGER     → cannot invite
USER        → cannot invite
```

An ADMIN must not be able to create a SUPER_ADMIN through the invitation API.

The backend must reject unauthorized role assignment with `403`.

Do not silently downgrade a requested role.

Verify the actual `UserController` before assuming the fix is present.

---

# 14. Tenant Onboarding

Tenant onboarding includes:

```text
POST /api/auth/signup
```

The first account is the tenant's SUPER_ADMIN.

Tenant provisioning must actually:

- validate tenant information
- create schema
- run tenant migrations
- register tenant
- configure initial state

A validation-only provisioning method is insufficient.

---

# 15. Invitations

Current teammate invitation endpoint:

```text
POST /api/users
```

Request conceptually:

```json
{
  "email": "user@example.com",
  "role": "USER"
}
```

Local-password invitation flow:

```text
ADMIN
  ↓
POST /api/users
  ↓
INVITED user
  ↓
random invite token
  ↓
7-day expiry
  ↓
POST /api/auth/accept-invite
  ↓
password created
  ↓
user ACTIVE
```

Email delivery is not currently a complete infrastructure feature, so the invite token may be returned directly for manual relay.

Google invite flow uses the same INVITED user record and activates upon successful Google login.

---

# 16. Invite Acceptance

Conceptual request:

```json
{
  "tenantId": "acme",
  "token": "...",
  "password": "password"
}
```

Invalid, expired, or already-used invite tokens must be rejected.

Do not allow an invite token to be reused.

---

# 17. Audit Logging

The system includes DB-backed per-tenant audit logging.

Audit records should remain tenant-scoped.

Future frontend activity/history screens should use backend audit data when available rather than fabricating records.

---

# 18. Billing Architecture

Billing uses Stripe.

Backend domain entities include:

```text
Customer
Subscription
Invoice
ProcessedWebhookEvent
```

Corresponding concepts include:

```text
PlanTier
SubscriptionStatus
InvoiceStatus
```

Stripe is server-side.

The frontend never communicates directly with Stripe APIs using secret credentials.

---

# 19. Stripe Checkout

Endpoint:

```text
POST /api/billing/checkout-session
```

Used by authorized billing users to begin an upgrade.

The backend:

- resolves current tenant
- resolves Stripe customer
- creates checkout session
- returns checkout URL

The frontend redirects to that URL.

---

# 20. Stripe Customer Portal

Endpoint:

```text
POST /api/billing/portal-session
```

The backend resolves the Stripe customer from the current tenant.

The client must not submit arbitrary Stripe customer IDs.

The backend returns a portal URL.

Frontend behavior:

```text
Manage Billing
      ↓
POST /api/billing/portal-session
      ↓
receive URL
      ↓
redirect browser
```

---

# 21. Stripe Webhooks

Supported webhook concepts include:

```text
checkout.session.completed
customer.subscription.created
customer.subscription.updated
customer.subscription.deleted
invoice.paid
invoice.payment_failed
```

Webhooks are:

- signature verified
- idempotent
- tenant-aware

Processed webhook events prevent duplicate processing.

Tenant resolution uses the appropriate Stripe identifiers.

Stripe API-version compatibility handling must remain intact where required.

Do not remove signature verification or idempotency for convenience.

---

# 22. Local Billing Synchronization

The backend synchronizes Stripe data into local tables.

Conceptually:

```text
Stripe Webhook
      ↓
BillingService
      ↓
Customer
Subscription
Invoice
      ↓
Local PostgreSQL
```

The frontend billing page should primarily read local billing data.

Do not call Stripe from the browser.

Do not call Stripe from the backend on every simple billing-summary GET if local synchronized data is sufficient.

---

# 23. Billing Summary

Reported endpoints:

```text
GET /api/billing
GET /api/billing/current
```

Verify actual implementation before frontend integration.

Conceptual response:

```json
{
  "plan": "PRO",
  "subscription": {
    "status": "ACTIVE",
    "stripeSubscriptionId": "sub_123",
    "stripePriceId": "price_pro",
    "currentPeriodStart": "2026-09-01T00:00:00Z",
    "currentPeriodEnd": "2026-10-01T00:00:00Z",
    "cancelAtPeriodEnd": false
  },
  "invoices": []
}
```

DTOs are the source of truth.

---

# 24. Billing Security

Billing must remain tenant-isolated.

Never accept:

```text
tenantId
schemaName
stripeCustomerId
```

from the frontend as authoritative billing context.

Use authenticated request + backend tenant context.

---

# 25. Billing Empty States

For an uninitialized tenant:

```text
plan = FREE
subscription = null
invoices = []
```

For a Stripe customer without a subscription:

```text
subscription = null
```

Do not display fake billing history.

---

# 26. Database

PostgreSQL contains:

### Public/global data

Tenant registry and other global infrastructure.

### Tenant data

Tenant-specific application and billing data.

Tenant migrations use Flyway.

Schema changes must be tracked through migrations rather than ad hoc database manipulation.

---

# 27. Known Technical Debt

These are known issues and should not be treated as urgent unless they directly block work:

### Invoice status default

An older migration leaves:

```text
DEFAULT 'PENDING'
```

but `PENDING` is not a real Stripe invoice status in the current enum.

Service-layer writes explicitly set the correct status.

### Timestamp inconsistency

Some billing tables contain older:

```text
TIMESTAMP
```

columns while newer parts of the application use:

```text
TIMESTAMPTZ
```

Do not perform unrelated cleanup during feature work.

---

# 28. Frontend Product Direction

The frontend should feel like a serious enterprise SaaS platform.

Visual reference:

- SchadenPro-style HR/workforce SaaS
- clean sidebar
- compact top header
- KPI cards
- charts
- employee management
- HRM navigation
- billing
- settings
- light/dark theme
- responsive layouts

Primary conceptual modules:

```text
Dashboard
Projects
Claims
Tasks
Schedule
Time Tracking
Reports
HRM
Billing
Settings
```

The complete frontend specification is maintained separately in:

```text
FRONTEND_SPEC.md
```

---

# 29. HRM Product Direction

HRM is the main operational product area represented in the UI direction.

Conceptual modules:

```text
Employee Management
Workforce Structure
Attendance & Leave
Work Schedule
Time Tracking
Documents
HR Reports
```

Conceptual relationship:

```text
Employee
    ↓
Team / Department
    ↓
Work Schedule
    ↓
Attendance
    ↓
Time Tracking
    ↓
Reports
```

The UI may expose future modules, but functionality should only be represented as live when supported by backend APIs.

---

# 30. Frontend / Backend Contract

The frontend should use:

```text
REST / JSON
```

against:

```text
http://localhost:8090
```

The frontend must:

- centralize API access
- handle authentication
- handle token refresh
- handle errors
- respect RBAC
- never expose secrets
- never control tenant schema
- never directly call Stripe

---

# 31. Configuration

Backend configuration should use environment variables/secrets for:

- JWT keys
- PostgreSQL credentials
- Redis credentials
- Stripe credentials
- Google OAuth credentials

Frontend configuration should only contain public configuration.

Never place backend secrets into Vite environment variables.

---

# 32. Docker

Development infrastructure uses Docker Compose for:

- PostgreSQL
- Redis
- other configured local services as applicable

Ports currently used:

```text
PostgreSQL → 5434
Redis      → 6380
Backend    → 8090
Frontend   → 5173
```

Keep local development configuration consistent.

---

# 33. Testing Philosophy

Testing is a major part of the project.

Backend:

- unit tests
- integration tests
- controller tests
- security tests
- webhook tests
- tenant isolation tests
- billing tests

Frontend:

- build
- lint
- component tests where justified
- API/auth flow verification
- protected-route verification
- responsive/manual verification

Do not remove tests simply to make builds pass.

---

# 34. Stripe Testing Philosophy

Stripe functionality should be testable without requiring live Stripe account operations wherever practical.

Existing architecture uses injectable wrapper beans around static Stripe SDK calls so Mockito can replace them in tests.

Webhook tests can use Stripe signature generation helpers.

Preserve this testing architecture.

---

# 35. Security Philosophy

Security is a core product requirement.

Important principles:

### Authentication

Users must be authenticated before accessing protected functionality.

### Authorization

Backend permissions are authoritative.

### Tenant isolation

One tenant must never access another tenant's data.

### Input validation

Validate request data server-side.

### Secrets

Never expose credentials.

### Stripe

All secret Stripe interaction is server-side.

### Webhooks

Verify signatures and enforce idempotency.

### Role assignment

Prevent privilege escalation.

---

# 36. Error Handling Philosophy

API errors should be predictable.

Important classes:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
409 Conflict
502 Bad Gateway
500 Internal Server Error
```

User-facing layers should provide useful messages without exposing:

- stack traces
- SQL
- secrets
- internal implementation details

---

# 37. Development Workflow

Use:

```text
Inspect
  ↓
Understand
  ↓
Plan
  ↓
Implement
  ↓
Test
  ↓
Verify
  ↓
Commit
```

Never make broad changes without inspecting the existing implementation.

When a bug is found:

1. reproduce
2. inspect root cause
3. fix root cause
4. add/regress a test
5. run relevant test suite

---

# 38. Agent Rules

Any coding agent working on this repository must follow these rules:

1. The repository is the source of truth.
2. Inspect before changing.
3. Do not invent APIs.
4. Do not invent DTO fields.
5. Do not rewrite working architecture without reason.
6. Do not perform unrelated cleanup during feature work.
7. Do not weaken security to simplify development.
8. Do not bypass tenant isolation.
9. Do not expose secrets.
10. Do not use frontend authorization as a substitute for backend authorization.
11. Do not replace real APIs with mocks when the real API exists.
12. Keep changes focused.
13. Add tests for security-sensitive behavior.
14. Run relevant verification after changes.
15. Do not stop at analysis when asked to implement.

---

# 39. Current Development State

Completed:

### Phase 1 — Skeleton

- Spring Boot backend
- React/Vite frontend scaffold
- PostgreSQL
- Redis
- Docker Compose
- CI

### Phase 2 — Multi-tenancy

- schema-per-tenant
- TenantContext
- tenant connection handling
- tenant provisioning
- tenant registry
- tenant migrations

### Phase 3 — Auth / RBAC / SSO / Audit

- JWT
- refresh rotation
- RBAC
- Google OIDC
- audit logging

### Phase 4 — Billing

- Stripe Checkout
- Stripe webhooks
- customers
- subscriptions
- invoices
- processed webhook events
- local synchronization
- billing self-service reportedly added

### Phase 5 — Onboarding

- signup
- tenant creation
- first SUPER_ADMIN
- teammate invitations
- invite acceptance
- Google invite flow
- role escalation fix reportedly added

### Current Phase

**Frontend product implementation**

The backend is substantially ahead of the frontend.

---

# 40. Current Frontend State

The repository already contains a Vite/React scaffold.

The frontend is not a finished UI.

The existing scaffold must be replaced/evolved into the actual product.

The frontend specification is maintained separately in:

```text
FRONTEND_SPEC.md
```

---

# 41. Product Evolution

Future areas can include:

```text
Employee Management
Attendance
Leave
Scheduling
Time Tracking
Projects
Claims
Tasks
Reports
Documents
Notifications
AI Insights
Automation
Advanced Billing
Analytics
```

Do not implement future modules as fake production functionality.

Build architecture that allows them to be added cleanly.

---

# 42. Definition of a High-Quality Feature

A feature is complete when:

- backend contract is verified
- authorization is correct
- tenant isolation is preserved
- validation exists
- UI is usable
- loading state exists
- empty state exists
- error state exists
- tests exist where appropriate
- build passes
- existing functionality remains intact

A feature is not complete merely because the happy path works.

---

# 43. Overall Quality Bar

The project should demonstrate:

### Backend engineering

- Spring Boot
- security
- multi-tenancy
- database design
- Redis
- Stripe
- testing
- migrations
- API design

### Frontend engineering

- React
- TypeScript
- component architecture
- routing
- authentication
- API integration
- responsive UI
- state management
- accessibility
- error handling

### System design

- tenant isolation
- clear service boundaries
- secure external integrations
- local synchronization
- testability
- maintainability

---

# 44. Final Principle

The project should behave like a real SaaS product.

Prefer:

```text
secure
explicit
testable
maintainable
tenant-safe
observable
```

over:

```text
quick
hardcoded
duplicated
mocked
implicit
```

When in doubt:

**Inspect the real code first, preserve existing architecture, make the smallest correct change, and verify it.**
