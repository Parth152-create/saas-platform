# Nexa — Phase 1 Production Hardening

## Objective

Harden the existing Nexa codebase to a near-production-ready state.

This phase focuses ONLY on application/codebase readiness.

### Out of scope

- Production domain
- DNS
- HTTPS infrastructure
- Live Stripe
- Production cloud deployment
- Kubernetes
- Multi-region infrastructure
- Enterprise compliance certifications

Those belong to later phases.

The goal is to make the existing Nexa application technically ready to move toward production without unnecessarily rewriting working functionality.

---

## 1. Current Architecture Audit

Before making changes, inspect the actual repository and document the current implementation of:

- Backend architecture
- Frontend architecture
- Authentication
- Authorization/RBAC
- Multi-tenancy
- PostgreSQL
- Redis
- Stripe integration
- File handling
- Email handling
- Observability
- Docker configuration
- Flyway migrations

Do not assume functionality exists simply because it is mentioned in documentation.

Use the existing implementation as the source of truth.

Before modifying any subsystem:

1. Locate the relevant code.
2. Understand the current architecture.
3. Reuse existing abstractions where appropriate.
4. Identify actual gaps.
5. Make the smallest clean change that closes the gap.

Do not introduce duplicate systems when an existing implementation can be hardened.

---

## 2. Security Hardening

### 2.1 Authentication

Audit and harden:

- Login
- Signup
- Google OIDC
- Access tokens
- Refresh tokens
- Refresh-token rotation
- Token expiration
- Logout/revocation
- Session expiration handling

Verify that:

- Expired access tokens are rejected.
- Invalid tokens are rejected.
- Refresh-token rotation behaves correctly.
- Revoked/invalid refresh tokens cannot be reused.
- Authentication failures do not expose sensitive information.
- Tokens and secrets are never written to logs.

Preserve the existing authentication architecture unless a concrete security issue requires a change.

### 2.2 Authorization and RBAC

Verify the existing role hierarchy:

- SUPER_ADMIN
- ADMIN
- MANAGER
- USER

Audit all protected APIs and ensure authorization is enforced server-side.

Do not rely on frontend permission checks for security.

Verify that users cannot:

- Access administrative APIs without permission.
- Modify resources they do not own or manage.
- Change their own role without authorization.
- Access higher-privilege functionality.
- Bypass authorization by manipulating request parameters.

Ensure role checks are applied consistently across controllers/services where required.

### 2.3 Multi-Tenancy

This is a critical security area.

Audit:

- TenantContext
- Tenant resolution
- Tenant identifier resolver
- Tenant connection/provider
- Tenant provisioning
- Tenant registry
- Repository access
- Background jobs
- WebSocket access
- File access
- Authentication tenant claims

Verify that a user from Tenant A cannot access Tenant B data by manipulating:

- IDs
- Request parameters
- Headers
- Query parameters
- URLs
- JWT-related inputs
- Resource identifiers

Test cross-tenant access attempts explicitly.

Tenant isolation must be enforced server-side.

### 2.4 API Security

Audit:

- CORS
- CSRF where applicable
- Security headers
- Request validation
- Path parameters
- Query parameters
- Request bodies
- Rate limiting
- Authentication bypasses
- Excessive data exposure
- HTTP method restrictions where applicable

Keep relaxed local-development behavior environment-specific.

### 2.5 Secrets and Configuration

Ensure:

- Secrets are not hardcoded.
- `.env` is not committed.
- Production secrets are externalized.
- `.env.example` contains placeholders only.
- API keys are not unnecessarily exposed to the frontend.
- Logs never expose secrets, passwords, tokens, or credentials.
- Development credentials are not accidentally used as production defaults.

Search the repository for obvious hardcoded credentials and suspicious secret-like values.

Do not modify legitimate test fixtures unnecessarily.

---

## 3. File Upload and Document Security

If Nexa supports file/document uploads, audit the implementation.

Verify or implement:

- Private object-storage abstraction
- Tenant-isolated storage paths
- File-size limits
- MIME/type validation
- Filename sanitization
- Safe download authorization
- Signed/private URLs
- Deletion
- Unauthorized access prevention

Never trust filenames or MIME types supplied by clients.

Production user files should not permanently depend on the application container's local filesystem.

If object storage is not yet implemented:

- Create a clean storage abstraction.
- Keep business logic independent from a specific storage provider.
- Make it ready for an object-storage provider later.
- Do not force a production provider into the project during this phase unless required by the existing architecture.

If file uploads are not currently implemented, document that fact instead of inventing functionality.

---

## 4. Email Infrastructure

Create or verify an email abstraction suitable for production integration.

Required use cases where applicable:

### Authentication

- Password reset
- Security notifications

### Organization

- Employee invitation
- Invitation reminder where applicable

### Account

- Important account/security events

Requirements:

- Provider-independent service interface.
- HTML/text templates where appropriate.
- Configuration through environment variables.
- Graceful failure handling.
- Logging without sensitive information.
- No hardcoded provider credentials.
- Clear separation between email composition and delivery.

Do not require a production email provider during Phase 1.

The application should be ready to connect one later.

If a complete email subsystem already exists, audit and harden it instead of replacing it.

---

## 5. Observability

Audit the existing observability implementation.

Verify:

- Structured application logs where appropriate.
- Correlation/request IDs.
- Useful error logging.
- `/actuator/health`.
- Readiness/liveness checks where appropriate.
- Database health visibility.
- Redis health visibility.
- Meaningful HTTP error responses.
- Frontend error handling.
- Important application events are diagnosable.

Production responses must not expose:

- Stack traces
- SQL queries
- Internal filesystem paths
- Secrets
- Access tokens
- Refresh tokens
- Database credentials
- Internal implementation details

Add useful application-level logging where genuinely needed.

Avoid excessive/noisy logging.

---

## 6. Database Reliability

Audit PostgreSQL and Flyway.

Verify:

- Every migration is deterministic.
- Migrations work on a fresh database.
- Existing migrations are not unnecessarily modified.
- Tenant provisioning works correctly.
- Tenant schema creation works correctly.
- Public tenant registry works correctly.
- Important queries have appropriate indexes.
- Foreign keys are appropriate.
- Unique constraints are correct.
- Transactions are correctly scoped.
- Connection pooling is production-safe.
- Database errors are handled cleanly.
- Database initialization does not depend on developer-specific assumptions.

Review all existing migrations before creating new ones.

Do not modify already-applied migrations in a way that can break existing environments.

Create new migrations when schema changes are genuinely required.

Pay particular attention to:

- Subscription records
- Tenant records
- User records
- HRM data
- Audit records
- Tenant-specific tables

Verify existing data remains compatible.

---

## 7. Production Configuration

Separate development, test, and production configuration cleanly.

Audit:

- Application properties
- Environment variables
- Docker configuration
- Frontend environment variables
- Backend configuration
- Logging configuration
- CORS origins
- Database configuration
- Redis configuration
- JWT configuration
- Google OAuth configuration
- Stripe configuration
- File storage configuration
- Email configuration

Ensure production configuration can be supplied entirely through environment variables/secrets.

Remove developer-specific assumptions such as:

- Hardcoded local filesystem paths
- Hardcoded localhost URLs where production values are required
- Hardcoded ports where environment configuration is expected
- Developer-specific credentials
- Local-only dependencies

Development defaults may remain where appropriate.

Do not break local development while introducing production configuration.

---

## 8. Frontend Hardening

Audit the React/TypeScript frontend and all major user flows.

Verify:

- Protected routes
- Authentication expiration
- Refresh-token behavior
- Logout
- Role-based UI
- Permission-based UI
- API error handling
- Loading states
- Empty states
- Network failures
- Unauthorized responses
- Session expiration
- Form validation
- File-upload errors
- Responsive behavior
- Production environment configuration

Frontend authorization must never be treated as the actual security boundary.

The backend remains responsible for authorization.

Ensure:

- API failures are presented clearly.
- Authentication failures do not leave users in broken states.
- Expired sessions recover cleanly.
- Sensitive information is not stored unnecessarily in browser storage.
- Production builds do not expose server secrets.

---

## 9. Standardized Error Handling

Audit and standardize backend API error handling using the project's existing conventions.

Verify appropriate handling for:

- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 409 Conflict
- 422 Unprocessable Entity where applicable
- 429 Too Many Requests
- 500 Internal Server Error

Responses should provide enough information for the frontend to respond appropriately without exposing implementation details.

Use the existing project's error-response architecture if one already exists.

Do not introduce a second competing error-handling architecture.

Production errors should be:

- Consistent
- Machine-readable where appropriate
- Safe
- Traceable through correlation IDs
- Useful to the frontend

---

## 10. Billing and Stripe Scope

Stripe is intentionally NOT being moved to production during Phase 1.

Keep existing Stripe architecture and test/development functionality intact.

Audit:

- Subscription lifecycle
- Entitlement resolution
- Webhook handling
- Webhook idempotency
- Subscription reconciliation
- Billing error handling
- Customer portal abstractions
- Plan/feature mapping

Verify development/test Stripe functionality does not create production assumptions.

Do not:

- Activate live Stripe.
- Add live Stripe credentials.
- Perform Stripe business/KYC onboarding.
- Configure production payment infrastructure.

The goal is for billing to remain architecturally ready while live payments are deferred.

---

## 11. Testing Strategy

Before changing implementation, inspect existing tests.

Do not create duplicate tests for functionality already adequately covered.

Add tests only where meaningful coverage is missing.

### Authentication

Cover where applicable:

- Login
- Invalid credentials
- Access-token expiration
- Refresh
- Refresh-token rotation
- Invalid refresh tokens
- Logout/revocation
- Authentication failures

### Authorization

Cover:

- SUPER_ADMIN permissions
- ADMIN permissions
- MANAGER permissions
- USER permissions
- Unauthorized API access
- Privilege escalation attempts

### Multi-Tenancy

Explicitly test:

- Tenant isolation
- Cross-tenant access attempts
- Tenant resolution
- Tenant provisioning
- Tenant-specific database access
- Tenant-specific resource access

A cross-tenant access attempt must be rejected.

### Billing

Continue covering:

- Subscription lifecycle
- Effective subscription resolution
- Entitlements
- Webhook idempotency
- Subscription reconciliation

Preserve existing regression coverage.

### File Tests

Where file functionality exists:

- Authorized upload
- Invalid file
- Oversized file
- Authorized download
- Unauthorized download
- Cross-tenant access
- Deletion

### API Tests

Cover:

- Validation failures
- Authentication failures
- Authorization failures
- Conflict responses
- Rate limiting where practical
- Expected error response structure

---

## 12. Code Quality

During hardening:

- Preserve existing architecture.
- Avoid unnecessary rewrites.
- Avoid introducing new frameworks without justification.
- Reuse existing services and utilities.
- Remove dead code discovered during the audit where safe.
- Avoid duplicate logic.
- Avoid hardcoded production values.
- Maintain package-by-feature architecture.
- Maintain Java 25 compatibility.
- Maintain React/TypeScript compatibility.
- Preserve existing API contracts unless a concrete issue requires a change.

Do not refactor unrelated functionality simply for stylistic reasons.

Do not replace working implementations merely because another architecture might be theoretically cleaner.

Prioritize correctness, security, maintainability, and production safety.

---

## 13. Docker and Build Verification

Audit the existing Docker configuration.

Verify:

- Backend Docker build works.
- Frontend Docker build works if applicable.
- Containers do not require developer-specific filesystem paths.
- Environment variables are externally configurable.
- Health checks are meaningful.
- Services fail clearly when required dependencies are unavailable.
- Production images do not contain unnecessary development artifacts.
- Secrets are not baked into images.

Do not redesign the entire Docker architecture.

Make only changes required for production readiness.

---

## 14. Dependency and Supply-Chain Review

Inspect backend and frontend dependencies.

Look for:

- Obsolete dependencies creating obvious security concerns.
- Unsafe configurations.
- Unnecessary dependencies.
- Duplicate libraries serving the same purpose.
- Development-only packages accidentally required at runtime.

Do not perform large dependency upgrades unless necessary.

If a dependency upgrade is needed:

1. Identify why.
2. Upgrade deliberately.
3. Run the full test suite.
4. Run frontend lint/build.
5. Verify application behavior remains intact.

---

## 15. Production Readiness Audit

After implementation, perform a second complete pass.

### Security

- [ ] Authentication hardened
- [ ] Authorization verified
- [ ] Tenant isolation verified
- [ ] Secrets protected
- [ ] API validation verified
- [ ] Rate limiting verified
- [ ] Security headers verified
- [ ] Sensitive data not exposed

### File Handling

- [ ] Storage abstraction exists or existing storage is production-safe
- [ ] File validation exists
- [ ] File size limits exist
- [ ] Tenant isolation exists
- [ ] Unauthorized downloads are prevented
- [ ] Production filesystem assumptions removed

### Email

- [ ] Email abstraction exists
- [ ] Invitation flow supported where applicable
- [ ] Password reset supported where applicable
- [ ] Provider configuration externalized
- [ ] Email failures handled safely

### Observability

- [ ] Correlation IDs
- [ ] Structured/useful logs
- [ ] Health checks
- [ ] Database health visibility
- [ ] Redis health visibility
- [ ] Safe production error responses
- [ ] No sensitive information in logs

### Database

- [ ] Migrations reviewed
- [ ] Fresh database initialization verified
- [ ] Tenant provisioning verified
- [ ] Indexes reviewed
- [ ] Transactions reviewed
- [ ] Connection pool reviewed
- [ ] Existing data compatibility verified

### Frontend

- [ ] Auth expiration handled
- [ ] Protected routes verified
- [ ] Permission UI verified
- [ ] API failures handled
- [ ] Loading states verified
- [ ] Empty states verified
- [ ] Production build succeeds

### Docker

- [ ] Backend image builds
- [ ] Frontend image builds where applicable
- [ ] No secrets baked into images
- [ ] No developer-specific paths
- [ ] Environment configuration externalized

### Testing

- [ ] Backend tests pass
- [ ] Frontend lint passes
- [ ] Frontend build passes
- [ ] Security-sensitive paths tested
- [ ] Multi-tenancy tests pass
- [ ] Existing regression tests remain green

---

## 16. Required Validation Commands

Run the project's actual validation commands after implementation.

Backend:

```bash
./mvnw clean test
```

Frontend:

```bash
npm run lint
npm run build
```

If Docker configuration is changed, also verify relevant Docker builds/compose configuration.

Do not report success based only on compilation.

Record:

- Test count
- Failures
- Errors
- Skipped tests
- Frontend lint result
- Frontend build result
- Docker build result where applicable

---

## 17. Explicitly Out of Scope

Do NOT implement during Phase 1:

- Production domain
- DNS
- Production TLS infrastructure
- Cloud deployment
- Live Stripe activation
- Stripe KYC/business onboarding
- Production payment processing
- Kubernetes
- Terraform
- Multi-region deployment
- CDN infrastructure
- Advanced autoscaling
- SOC 2
- ISO 27001
- Enterprise SSO/SAML
- Advanced analytics infrastructure
- Multi-region disaster recovery
- Full enterprise compliance program
- Dedicated DevOps infrastructure

Stripe test/development functionality may remain.

The application should be prepared for these later phases without prematurely implementing them.

---

## 18. Implementation Rules for the Coding Agent

### Inspect First

Before modifying any file:

- Inspect the relevant implementation.
- Inspect related tests.
- Inspect configuration.
- Inspect existing documentation where useful.
- Understand how the current system works.

### Do Not Guess

Do not assume a feature is missing because it is not obvious from the README.

Search the repository.

Do not invent APIs, classes, environment variables, or infrastructure that already exists.

### Preserve Working Functionality

The existing Nexa application already contains substantial functionality.

Do not rewrite working systems without a concrete reason.

### Prefer Incremental Changes

For every change:

1. Identify the gap.
2. Make the smallest appropriate change.
3. Add/update tests.
4. Run validation.
5. Verify existing functionality.

### Avoid Scope Creep

Do not add unrelated features.

Do not redesign the UI unless required for production hardening.

Do not redesign the architecture unless required to address a real problem.

Do not activate external production services.

### Documentation

Update documentation only when implementation changes make existing documentation inaccurate.

Do not create large documentation sections for functionality that does not exist.

---

## 19. Final Deliverable

At the end of Phase 1, produce a report with:

### Implementation Summary

Describe:

- What was audited
- What was changed
- What was intentionally left unchanged
- Security improvements
- Multi-tenancy improvements
- File-storage improvements
- Email improvements
- Observability improvements
- Database improvements
- Frontend improvements
- Configuration improvements

### Files Changed

List all changed files and briefly explain why each changed.

### Security Findings

For each finding:

- Severity
- Problem
- Resolution
- Relevant file(s)

If no significant issue was found in an area, explicitly state that it was audited and no change was required.

### Tests Added or Modified

List:

- Test class
- Test purpose
- Important scenarios covered

### Validation Results

Report actual results:

```text
Backend tests:
Frontend lint:
Frontend build:
Docker build:
```

Do not claim a check passed unless it was actually executed.

### Remaining Risks

List remaining technical risks or known limitations.

Clearly distinguish:

- Critical
- High
- Medium
- Low
- Deferred

### Deferred to Phase 2

Explicitly list work intentionally postponed, especially:

- Domain
- DNS
- HTTPS infrastructure
- Cloud deployment
- Production database hosting
- Production Redis
- Production object storage
- Production email provider
- Live Stripe

---

## 20. Completion Criteria

Phase 1 is complete only when:

1. Existing functionality still works.
2. Backend tests pass.
3. Frontend lint passes.
4. Frontend production build passes.
5. Authentication has been audited.
6. Authorization has been audited.
7. Tenant isolation has been audited.
8. File handling is production-safe or has a production-ready storage abstraction.
9. Email functionality is provider-ready.
10. Observability is sufficient to diagnose production failures.
11. Database migrations and tenant provisioning have been verified.
12. Production configuration contains no developer-specific assumptions.
13. No secrets are committed.
14. No sensitive information is exposed through logs or API errors.
15. Existing billing/test Stripe functionality remains intact.
16. No unnecessary architectural rewrites were introduced.
17. No critical security, tenant-isolation, authentication, or data-integrity issue remains unresolved.

Phase 1 should result in a codebase that is technically ready to move into a later deployment/infrastructure phase.

It should NOT be described as fully production-deployed until the later infrastructure phase is complete.
