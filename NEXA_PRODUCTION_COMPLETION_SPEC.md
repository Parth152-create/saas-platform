# Nexa — Production Completion Specification
## Product Integration, HRM, User Management, Billing, Webhooks, Docker, Secrets, Auth Hardening & Observability

### Scope

This specification covers the next production-completion phase of Nexa.

Included:

1. Subscription → actual product enforcement verification
2. Complete frontend ↔ backend API integration
3. Complete HRM backend/frontend workflows
4. User management & invitation end-to-end verification
5. Real Stripe billing lifecycle verification
6. Webhook idempotency hardening
7. Full Docker Compose application stack
8. Environment variables & secret management
9. Production authentication/security hardening
10. Observability, health checks, logging and operational visibility

Do NOT redesign the landing page or branding in this phase.

Do NOT introduce unrelated product features.

The repository is the source of truth.

---

# 1. Non-Negotiable Architecture

Preserve the existing architecture:

```text
Nexa
 │
 ├── React + TypeScript + Vite
 │
 └── Spring Boot + Java
       │
       ├── PostgreSQL
       │
       ├── Redis
       │
       └── Stripe
```

Multi-tenancy:

```text
Global public schema
       │
       └── tenant_registry
               │
       ┌───────┼────────┐
       ↓       ↓        ↓
   Tenant A Tenant B Tenant C
   Schema A Schema B Schema C
```

Authentication:

```text
JWT
 ↓
Tenant Resolution
 ↓
RBAC
 ↓
Subscription Entitlement
 ↓
Tenant Data
```

Do not replace schema-per-tenant with shared `tenant_id` tables.

Do not replace RBAC with subscription checks.

Do not make frontend authorization the security boundary.

---

# 2. Subscription → Actual Product Enforcement

The subscription system already exists. The next task is to verify that its entitlements actually control real application capabilities.

Required flow:

```text
Stripe Subscription
       ↓
Tenant Subscription
       ↓
Plan
       ↓
Feature Entitlements
       ↓
Backend Authorization
       ↓
Frontend Visibility
       ↓
Actual Feature/API
```

A feature is not considered complete merely because it is hidden in React.

The backend API that performs the feature must also enforce the entitlement.

Example:

```text
STARTER
 ├── Employee Management ✓
 ├── Projects ✓
 ├── Tasks ✓
 ├── Time Tracking ✓
 ├── Basic Reports ✓
 ├── Advanced Reports ✗
 ├── Advanced Analytics ✗
 └── Custom Workflows ✗
```

Verify every feature currently declared by the entitlement system.

For every gated feature:

```text
Frontend visible?
        ↓
API exists?
        ↓
API connected?
        ↓
Backend entitlement check?
        ↓
RBAC check?
        ↓
Tenant isolation?
```

A user must not gain access by directly calling the API.

---

# 3. Complete Frontend ↔ Backend Integration

Perform a systematic audit of every major application page.

Pages/modules:

```text
Dashboard
Projects
Claims / Expenses
Tasks
Schedule
Time Tracking
Reports & Analytics
HR Overview
Employee Directory
Teams & Departments
Attendance & Leave
Documents & Files
Settings
Subscription
Billing
User Management
Roles & Permissions
Integrations
```

For each page determine:

```text
UI
 ↓
API client
 ↓
Controller endpoint
 ↓
Service
 ↓
Database
 ↓
Real tenant data
```

Replace UI-only/mock behavior where a real backend capability exists.

Do not delete demo data that is intentionally used for product visualization on the public landing page.

Inside the authenticated application, do not present fake data as real tenant data.

---

## API Integration Standards

Use a centralized API client.

Do not scatter raw `fetch()` calls throughout components if the project already has an API abstraction.

Every API-driven page should support:

```text
Loading
Error
Empty
Success
Unauthorized
Forbidden
```

For mutations:

```text
Submitting
Success
Validation Error
Server Error
```

Do not silently swallow API errors.

---

# 4. HRM End-to-End Workflows

The HRM module must operate on real tenant-scoped backend data.

Required areas:

```text
Employee Management
Teams & Departments
Attendance
Leave
Work Schedules
Time Tracking
Documents
```

## Employee lifecycle

```text
ADMIN
  ↓
Create Employee
  ↓
Tenant Database
  ↓
Employee Directory
  ↓
Employee Profile
  ↓
Edit
  ↓
Archive/Delete where supported
```

Verify:

- Create
- Read/list
- Search
- Filter
- Update
- Status changes
- Appropriate delete/archive behavior
- Tenant isolation
- RBAC

---

## Employee profile

Profile should connect to real backend data where endpoints exist.

Potential sections:

```text
Personal Information
Employment
Department
Role
Attendance
Leave
Time
Documents
```

Do not create backend functionality that is not required unless the repository clearly needs it.

---

## Teams & Departments

Required flow:

```text
Create Department
      ↓
Assign Employees
      ↓
View Department
      ↓
Update
      ↓
Remove/reassign
```

Ensure all operations are tenant-scoped.

---

## Attendance & Leave

Connect frontend to real backend endpoints.

Support the workflows that already exist in the backend.

Do not fabricate attendance records.

Handle:

```text
No records
Loading
Error
Date filtering
Status
```

---

## Work Schedules

Connect schedule configuration to the backend.

Verify that only authorized roles can modify organizational schedules.

---

## Documents

If document persistence/upload is not actually implemented, do NOT fake successful uploads.

Instead:

- Use existing backend functionality if available.
- If unavailable, clearly keep the UI in a not-yet-connected state rather than creating fake persistence.

---

# 5. User Management & Invitations

Verify the complete user lifecycle.

```text
ADMIN
 ↓
User Management
 ↓
Invite User
 ↓
Invitation
 ↓
Accept Invite
 ↓
Create Credentials
 ↓
Tenant Membership
 ↓
Role
 ↓
Login
 ↓
Correct Permissions
```

Test all roles:

```text
SUPER_ADMIN
ADMIN
MANAGER
USER
```

Verify the hierarchy remains:

```text
SUPER_ADMIN
     >
ADMIN
     >
MANAGER
     >
USER
```

## Escalation security

Must remain:

```text
SUPER_ADMIN → can invite SUPER_ADMIN
ADMIN       → cannot invite SUPER_ADMIN
```

A frontend dropdown restriction is insufficient.

The backend must enforce the rule.

---

## Invitation security

Verify:

- Token validation
- Expiration
- Single-use behavior where implemented
- Correct tenant
- Correct role
- Password hashing
- No cross-tenant invitation acceptance

Test:

```text
Tenant A invitation
       ≠
Tenant B account
```

---

# 6. Real Stripe Billing Lifecycle

Use Stripe test mode for end-to-end verification.

Do not replace existing billing architecture.

Required lifecycle:

```text
User
 ↓
Signup
 ↓
Select Plan
 ↓
Checkout
 ↓
Stripe
 ↓
Webhook
 ↓
Tenant Subscription
 ↓
Plan
 ↓
Feature Entitlements
 ↓
Application Access
```

Verify at minimum:

### New subscription

```text
Checkout completed
→ subscription created
→ webhook received
→ tenant updated
→ correct plan
→ correct features
```

### Upgrade

```text
STARTER
 ↓
Upgrade
 ↓
Stripe
 ↓
Webhook
 ↓
PRO
 ↓
PRO entitlements
```

### Downgrade

```text
PRO
 ↓
Downgrade
 ↓
Webhook
 ↓
STARTER
 ↓
Premium entitlements revoked
```

### Cancellation

```text
PRO
 ↓
Cancel
 ↓
Stripe webhook
 ↓
Subscription inactive/cancelled
 ↓
Premium features revoked
```

### Invoice

Verify:

- Invoice created
- Invoice synchronized
- Status synchronized
- Amount/currency displayed
- Hosted invoice URL where available
- PDF URL where available

Never create fake invoices.

---

# 7. Webhook Idempotency Hardening

Existing protection uses the `processed_webhook_events` database table.

Preserve it.

Current conceptual flow:

```text
Receive webhook
 ↓
Check processed event
 ↓
Process
 ↓
Save processed event
```

Harden this so concurrent duplicate webhook deliveries cannot execute the billing action twice.

Preferred approaches:

### Option A — Atomic database claim

Use the existing unique/primary key:

```text
stripe_event_id
```

Attempt to insert/claim the event atomically.

Only the transaction that successfully claims the event may process it.

### Option B — Redis atomic claim

Use an atomic Redis operation such as:

```text
SET key value NX EX ...
```

Then retain the database record as durable processing history.

Do not implement two independent idempotency systems that can disagree.

Choose one authoritative processing-claim mechanism and document it.

---

## Webhook requirements

Continue verifying:

- Stripe signature
- Event type
- Tenant resolution
- Customer mapping
- Checkout tenant mapping
- Processed-event handling
- Tenant context cleanup

Always clear tenant context in `finally`.

Never trust tenant identifiers supplied by an unverified client.

---

# 8. Full Docker Compose Stack

Current Compose provides PostgreSQL and Redis.

Extend it carefully so the complete local application can be started consistently.

Desired:

```text
docker compose up
```

starts:

```text
postgres
redis
backend
frontend
```

Conceptual:

```text
                  Docker Compose
                       │
       ┌───────────────┼───────────────┐
       ↓               ↓               ↓
   PostgreSQL         Redis          Backend
                                       │
                                       ↓
                                    Frontend
```

Preferred service names:

```text
postgres
redis
backend
frontend
```

Use existing ports/conventions unless there is a strong reason to change them.

Current known ports:

```text
Backend:    8090
PostgreSQL: 5434 host mapping
Redis:      6380 host mapping
Frontend:   5173
```

Inside Docker networking, services should communicate using service names, not host `localhost`.

Example:

```text
backend → postgres:5432
backend → redis:6379
```

---

## Docker health checks

Add appropriate health checks.

PostgreSQL:

```text
pg_isready
```

Redis:

```text
redis-cli ping
```

Backend:

Use the application's health endpoint if available.

Frontend:

Use an appropriate lightweight HTTP check if practical.

Use `depends_on` with health conditions where supported by the current Compose implementation.

Do not use arbitrary sleep commands as the primary readiness mechanism.

---

# 9. Environment Variables & Secret Management

Create or update:

```text
.env.example
```

Never commit real secrets.

Expected categories:

### Database

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
DATABASE_URL
```

### Redis

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
```

### JWT

```text
JWT_PRIVATE_KEY
JWT_PUBLIC_KEY
JWT_ISSUER
```

Use the project's actual property names rather than blindly introducing duplicates.

### Stripe

```text
STRIPE_SECRET_KEY
STRIPE_WEBHOOK_SECRET
STRIPE_PRICE_*
```

Never expose secret Stripe keys to React.

Only publishable Stripe values may be exposed to frontend code when actually required.

### Google OIDC

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
```

### Frontend

```text
VITE_API_BASE_URL
```

Use only `VITE_` variables that are safe to expose publicly.

---

## Secret checks

Before completion, search the repository for accidental secrets:

```text
sk_live_
sk_test_
whsec_
private keys
database passwords
client secrets
JWT private key material
```

Do not print secrets into logs.

---

# 10. Production Authentication & Security Hardening

The current architecture already uses JWT, RS256/Nimbus, refresh-token rotation, tenant-aware authentication, and RBAC.

Do not replace this architecture unnecessarily.

Perform a security audit.

## Access tokens

Verify:

- Signature validation
- Expiration
- Issuer validation
- Algorithm restrictions
- Required claims
- Tenant resolution
- Role resolution

Never trust:

```text
tenant_id
role
plan
```

from request bodies or arbitrary headers.

---

## Refresh tokens

Verify:

```text
Refresh token
 ↓
Signature validation
 ↓
Expiration
 ↓
Redis/session validation
 ↓
Rotation
 ↓
Old token invalidated
 ↓
New token issued
```

Test replay of an old refresh token.

It must not silently create another valid session if rotation semantics require reuse detection.

---

## Login abuse protection

Verify existing per-IP rate limiting.

Ensure it applies to:

- Login
- Potentially refresh
- Other sensitive authentication endpoints where appropriate

Do not lock legitimate users out unnecessarily.

---

## Password handling

Verify:

- Strong BCrypt hashing
- Never store plaintext passwords
- Never log passwords
- Validation errors do not expose sensitive information

---

## CORS

Development:

```text
localhost:5173
```

Production:

Only allow explicitly configured frontend origins.

Do not use:

```text
*
```

with credentials.

---

## Error handling

Do not expose:

- Stack traces
- SQL errors
- Internal filesystem paths
- JWT internals
- Database credentials
- Stripe secrets

to clients.

---

## Security headers

Review whether the deployment/application should provide:

- Content Security Policy
- X-Content-Type-Options
- Referrer-Policy
- Frame protection
- Strict Transport Security in HTTPS production

Do not enable settings blindly if they break the existing application. Test them.

---

# 11. Observability & Operational Visibility

Add a lightweight production-ready observability layer.

Do not over-engineer this into a full enterprise monitoring platform.

## Health

Expose/verify health information for:

```text
Application
PostgreSQL
Redis
```

Use Spring Boot Actuator if it is already available or can be introduced consistently.

Separate:

```text
liveness
readiness
```

where practical.

---

## Logging

Use structured, useful application logs.

Important events:

```text
Application startup
Database connection
Redis connection
Authentication failures
Authorization failures
Tenant resolution failures
Invitation events
Subscription changes
Webhook processing
Webhook failures
Unexpected exceptions
```

Do NOT log:

- Passwords
- JWT tokens
- Refresh tokens
- Stripe secret keys
- Database passwords
- Full sensitive request bodies

---

## Correlation/request IDs

Add a request/correlation ID where practical.

Conceptual:

```text
Request
 ↓
X-Request-ID
 ↓
Controller
 ↓
Service
 ↓
Database/Stripe/Redis
```

This should make debugging one request possible across logs.

Do not leak sensitive information into the request ID.

---

## Metrics

At minimum, expose useful operational metrics if the existing stack supports them:

```text
HTTP request count
HTTP latency
HTTP error count
Authentication failures
Webhook processing count
Webhook failures
Database health
Redis health
```

Do not add complex dashboards unless required.

---

# 12. End-to-End Test Matrix

Create a final workflow test matrix.

## Authentication

```text
✓ Signup
✓ Login
✓ Refresh
✓ Refresh token rotation
✓ Invalid JWT
✓ Expired JWT
✓ Wrong tenant
✓ Google OIDC where configured
✓ Accept invite
```

## RBAC

```text
✓ SUPER_ADMIN
✓ ADMIN
✓ MANAGER
✓ USER
✓ Unauthorized action
✓ SUPER_ADMIN escalation restriction
```

## Multi-tenancy

```text
✓ Tenant A sees Tenant A data
✓ Tenant B sees Tenant B data
✓ Tenant A cannot read Tenant B
✓ Tenant A cannot modify Tenant B
✓ Subscription isolation
```

## Subscription

```text
✓ STARTER entitlements
✓ PRO entitlements
✓ ENTERPRISE entitlements
✓ Upgrade
✓ Downgrade
✓ Cancellation
✓ Feature revocation
```

## Billing

```text
✓ Checkout
✓ Customer portal
✓ Webhook signature
✓ Duplicate webhook
✓ Invoice synchronization
✓ Subscription synchronization
```

## HRM

```text
✓ Employee CRUD
✓ Team/department workflow
✓ Attendance
✓ Leave
✓ Work schedule
✓ Time tracking
✓ Documents where backend support exists
```

## Frontend

```text
✓ Loading states
✓ Error states
✓ Empty states
✓ 401 handling
✓ 403 handling
✓ Responsive UI
```

---

# 13. Test Commands

Use the project's actual commands.

Backend:

```bash
./mvnw test
```

Frontend:

```bash
npm run lint
npm run build
```

Docker:

```bash
docker compose config
docker compose up --build
```

Verify service health after startup.

Do not report success unless commands actually pass.

---

# 14. Implementation Order

Do NOT implement everything in one uncontrolled change.

Use this order:

### Phase A — Integration audit

```text
Inspect APIs
 ↓
Map frontend pages
 ↓
Identify mocks
 ↓
Identify missing connections
```

### Phase B — Core frontend integration

```text
Dashboard
Projects
Tasks
Claims
Schedule
Time
Reports
```

### Phase C — HRM

```text
Employees
Teams
Attendance
Leave
Schedules
Documents
```

### Phase D — User lifecycle

```text
User Management
Invitations
Accept Invite
RBAC
```

### Phase E — Billing

```text
Checkout
Subscription
Invoices
Portal
Upgrade
Downgrade
Cancellation
```

### Phase F — Webhook hardening

```text
Atomic event claiming
Duplicate delivery tests
Concurrent processing tests
```

### Phase G — Infrastructure

```text
Docker
Environment variables
Health checks
```

### Phase H — Security

```text
JWT
Refresh tokens
CORS
Rate limiting
Secrets
Error handling
Headers
```

### Phase I — Observability

```text
Health
Logs
Request IDs
Metrics
```

### Phase J — Final verification

```text
Backend tests
Frontend lint
Frontend build
Docker startup
End-to-end workflows
```

---

# 15. Rules for the Coding Agent

Before changing code:

1. Inspect the actual repository.
2. Inspect backend controllers/services/DTOs.
3. Inspect frontend API clients.
4. Inspect existing tests.
5. Identify existing functionality before creating new functionality.
6. Reuse existing architecture.
7. Make small coherent changes.
8. Run tests after each major phase.

Do NOT:

- Rewrite working backend architecture
- Replace schema-per-tenant
- Replace RBAC
- Create fake APIs
- Create fake billing data
- Hardcode subscription authorization in React
- Put Stripe secrets in frontend
- Duplicate business logic unnecessarily
- Delete existing tests
- Disable tests to make builds pass
- Hide errors with empty catches
- Use `localhost` between Docker services
- Commit `.env` secrets

---

# 16. Definition of Done

This phase is complete when:

## Product

```text
✓ Major frontend pages use real backend APIs where functionality exists
✓ Authenticated UI no longer presents mock data as real tenant data
✓ HRM workflows are connected
✓ User invitation flow is verified
✓ RBAC remains enforced
✓ Subscription entitlements control actual APIs
```

## Billing

```text
✓ Checkout verified
✓ Subscription synchronization verified
✓ Invoice synchronization verified
✓ Customer portal verified
✓ Upgrade verified
✓ Downgrade verified
✓ Cancellation verified
✓ Webhook signature verified
✓ Duplicate webhook protection hardened
```

## Infrastructure

```text
✓ Docker Compose starts required services
✓ PostgreSQL health check
✓ Redis health check
✓ Backend health check
✓ Frontend starts
✓ Service-to-service networking works
✓ .env.example exists
✓ No secrets committed
```

## Security

```text
✓ JWT validation
✓ Refresh rotation
✓ Tenant isolation
✓ RBAC
✓ Subscription authorization
✓ Rate limiting
✓ CORS
✓ Safe error responses
✓ Sensitive data not logged
```

## Observability

```text
✓ Health checks
✓ Useful structured logs
✓ Request/correlation IDs where practical
✓ Operational metrics where supported
```

## Verification

```text
✓ ./mvnw test
✓ npm run lint
✓ npm run build
✓ docker compose config
✓ docker compose up --build
✓ End-to-end workflow verification
```

---

# 17. Final Target Architecture

```text
                              NEXA
                               │
                ┌──────────────┴──────────────┐
                │                             │
             PUBLIC                        AUTHENTICATED
             LANDING                          APP
                │                             │
             Signup                         JWT
                │                             │
             Stripe                     Tenant Context
                │                             │
          Subscription                       RBAC
                │                             │
               Plan                     Feature Entitlement
                │                             │
          Entitlements                   Tenant Schema
                │                             │
                └──────────────┬──────────────┘
                               │
                        PostgreSQL + Redis
                               │
                         Stripe Webhooks
                               │
                         Observability
                               │
                           Docker
```

The final goal is not simply a working UI.

The goal is a coherent SaaS product where:

```text
Business
   ↓
Subscription
   ↓
Entitlements
   ↓
Users + Roles
   ↓
Authorized Features
   ↓
Tenant-Isolated Data
```

all work together through the same backend source of truth.
