# SaaS Platform — Security Fix + Billing Self-Service

Repository: `https://github.com/Parth152-create/saas-platform`

## Context

You are working on the existing Spring Boot + React multi-tenant SaaS platform. **Do not redesign the architecture or introduce unrelated changes.** First inspect the repository and understand the existing authentication, RBAC, tenant-context, Stripe billing, webhook, repository, DTO, and test patterns. Implement the two tasks below using the project's existing conventions.

The repository currently has:

- Roles: `SUPER_ADMIN`, `ADMIN`, `MANAGER`, `USER`.
- Role hierarchy: `SUPER_ADMIN > ADMIN > MANAGER > USER`.
- User invitation endpoint: `POST /api/users`.
- The invitation endpoint currently has `@PreAuthorize("hasRole('ADMIN')")` and accepts the requested `Role` directly, which means an `ADMIN` can currently submit `SUPER_ADMIN`.
- Stripe checkout already exists at `POST /api/billing/checkout-session`.
- Stripe customer IDs are stored on the tenant registry.
- Local billing persistence already exists for customers, subscriptions, and invoices.
- Stripe webhook processing already synchronizes subscription and invoice data into the local database.

Relevant existing files include:

- `backend/src/main/java/com/yourco/saas/users/UserController.java`
- `backend/src/main/java/com/yourco/saas/users/dto/InviteUserRequest.java`
- `backend/src/main/java/com/yourco/saas/users/dto/InviteUserResponse.java`
- `backend/src/main/java/com/yourco/saas/domain/user/Role.java`
- `backend/src/main/java/com/yourco/saas/domain/user/User.java`
- `backend/src/main/java/com/yourco/saas/domain/user/UserRepository.java`
- `backend/src/main/java/com/yourco/saas/auth/AuthController.java`
- `backend/src/main/java/com/yourco/saas/config/SecurityConfig.java`
- `backend/src/main/java/com/yourco/saas/billing/BillingController.java`
- `backend/src/main/java/com/yourco/saas/billing/BillingService.java`
- `backend/src/main/java/com/yourco/saas/billing/StripeConfig.java`
- `backend/src/main/java/com/yourco/saas/billing/StripeProperties.java`
- `backend/src/main/java/com/yourco/saas/domain/billing/Customer.java`
- `backend/src/main/java/com/yourco/saas/domain/billing/Subscription.java`
- `backend/src/main/java/com/yourco/saas/domain/billing/Invoice.java`
- `backend/src/main/java/com/yourco/saas/domain/billing/SubscriptionRepository.java`
- `backend/src/main/java/com/yourco/saas/domain/billing/InvoiceRepository.java`
- `backend/src/main/java/com/yourco/saas/tenant/TenantRegistryService.java`
- existing RBAC and billing tests.

---

# Task 1 — Fix ADMIN → SUPER_ADMIN role escalation

## Current vulnerability

`POST /api/users` is protected with `hasRole('ADMIN')`, which intentionally allows both `ADMIN` and `SUPER_ADMIN` because of the configured role hierarchy.

However, the request contains an arbitrary `Role`, and the controller currently passes that role directly into:

```java
User.newInvitedUser(request.email(), request.role(), token, expiresAt)
```

Therefore an authenticated `ADMIN` can currently invite a user with `SUPER_ADMIN`.

This must be fixed at the authorization/business-rule layer. Do **not** solve it by breaking the existing role hierarchy.

## Required behavior

### SUPER_ADMIN

A `SUPER_ADMIN` may invite users with any valid role:

- `SUPER_ADMIN`
- `ADMIN`
- `MANAGER`
- `USER`

### ADMIN

An `ADMIN` may invite:

- `ADMIN`
- `MANAGER`
- `USER`

An `ADMIN` must **not** be able to invite:

- `SUPER_ADMIN`

The request must be rejected before creating/saving the invited user.

Prefer a clear `403 FORBIDDEN` response for an authorization violation, consistent with the application's existing exception/security style.

Do not silently downgrade the requested role to `ADMIN`. Reject the request.

### MANAGER / USER

They must remain unable to use the invitation endpoint, exactly as before.

## Implementation requirements

1. Inspect how the current authenticated user's role is represented and retrieved.
2. Reuse the existing Spring Security authentication/context rather than introducing a second authentication mechanism.
3. Add the smallest clean authorization check needed.
4. Keep `@PreAuthorize("hasRole('ADMIN')")` or an equivalent authorization boundary so non-admin users remain blocked.
5. Do not duplicate role hierarchy configuration.
6. Do not add a new database migration for this fix.
7. Preserve all existing invitation behavior:
   - duplicate-email protection
   - invite token generation
   - seven-day expiration
   - `INVITED` status
   - existing response DTO
   - existing accept-invite flow

## Tests required

Add/update tests covering at minimum:

1. `SUPER_ADMIN` can invite `SUPER_ADMIN`.
2. `SUPER_ADMIN` can invite `ADMIN`.
3. `ADMIN` can invite `ADMIN`.
4. `ADMIN` can invite `MANAGER`.
5. `ADMIN` can invite `USER`.
6. `ADMIN` attempting to invite `SUPER_ADMIN` receives `403`.
7. `MANAGER` attempting to invite any role remains forbidden.
8. `USER` attempting to invite any role remains forbidden.
9. Existing successful invitation/acceptance flow still passes.

Use the existing test infrastructure and helpers instead of creating a new testing framework.

---

# Task 2 — Billing self-service APIs

The platform already has Stripe checkout and webhook synchronization. Extend the existing billing implementation instead of creating a parallel billing system.

The goal is to expose:

1. A Stripe Customer Portal session endpoint.
2. A read-only endpoint returning the tenant's current billing state:
   - current plan
   - current subscription
   - invoices

These APIs will be consumed by the frontend later.

---

## 2A. Stripe Customer Portal session

Add an authenticated endpoint under the existing `/api/billing` controller.

Recommended endpoint:

```http
POST /api/billing/portal-session
```

Response:

```json
{
  "url": "https://billing.stripe.com/..."
}
```

Use the Stripe Java SDK version already present in the project. Do not add another Stripe dependency/version.

### Authorization

Billing self-service must be restricted to an appropriate tenant administrator.

Use the existing authorization model. The intended behavior is:

- `SUPER_ADMIN` → allowed
- `ADMIN` → allowed
- `MANAGER` → forbidden
- `USER` → forbidden

Do not create a new role.

### Tenant/customer resolution

Resolve the current tenant using the existing `TenantContext` / tenant registry mechanism.

The Stripe customer must come from the tenant's existing `stripe_customer_id`.

Do **not** accept a Stripe customer ID from the request body.

This is important because a client must never be able to select another tenant's Stripe customer.

### No Stripe customer

If the current tenant has no Stripe customer ID yet, return a clean `400 Bad Request` (or the project's established equivalent) explaining that billing has not been initialized / no Stripe customer exists.

Do not create a new Stripe customer as a side effect unless the existing architecture already explicitly does that.

### Stripe API call

Use Stripe Billing Portal's session creation API and configure the existing application/frontend return URL according to the project's configuration conventions.

If the project does not already have a dedicated billing-portal return URL, add a configuration property using the existing Stripe properties pattern rather than hardcoding a production URL.

For example, follow the existing `StripeProperties` style and environment-variable configuration.

Return only the portal URL needed by the frontend.

Do not expose Stripe secrets or internal customer IDs.

### Error handling

Follow the existing Stripe exception handling pattern used by `BillingController`.

Stripe failures should become an appropriate `502 Bad Gateway`/existing upstream-service error rather than exposing raw Stripe internals.

---

# 2B. GET current billing information

Add a read-only endpoint under `/api/billing`.

Recommended endpoint:

```http
GET /api/billing
```

If the existing API naming conventions strongly suggest a more explicit path such as `/api/billing/current`, use that instead — but keep the endpoint intuitive and consistent.

The endpoint should return the current tenant's billing state.

Recommended response shape:

```json
{
  "plan": "PRO",
  "subscription": {
    "status": "ACTIVE",
    "stripeSubscriptionId": "sub_...",
    "stripePriceId": "price_...",
    "currentPeriodStart": "2026-09-01T00:00:00Z",
    "currentPeriodEnd": "2026-10-01T00:00:00Z",
    "cancelAtPeriodEnd": false
  },
  "invoices": [
    {
      "stripeInvoiceId": "in_...",
      "status": "PAID",
      "amountDueCents": 2900,
      "amountPaidCents": 2900,
      "currency": "usd",
      "hostedInvoiceUrl": "https://invoice.stripe.com/...",
      "invoicePdfUrl": "https://...",
      "paidAt": "2026-09-01T10:00:00Z"
    }
  ]
}
```

Adapt the exact DTO/property names to the existing project's Java naming and JSON conventions.

## Important: source of truth

Use the existing local billing database records as the primary source for this GET endpoint.

Do not make unnecessary Stripe API calls on every GET.

The existing webhook flow already persists:

- customer
- subscription
- plan tier
- Stripe price
- subscription period
- cancellation-at-period-end
- invoice status
- invoice amounts
- currency
- hosted invoice URL
- invoice PDF URL
- paid timestamp

Reuse that data.

The existing entities/repositories already support this:

- `Customer`
- `Subscription`
- `Invoice`
- `SubscriptionRepository`
- `InvoiceRepository`

Do not duplicate these entities or create another billing table.

## Tenant isolation

This endpoint must never return billing information belonging to another tenant.

The safe resolution path should be conceptually:

```text
TenantContext
    ↓
TenantRegistryService
    ↓
current tenant's stripe_customer_id
    ↓
CustomerRepository
    ↓
current customer's local subscription/invoices
```

Do not accept:

- `tenantId`
- `stripeCustomerId`
- `customerId`
- `subscriptionId`

as user-controlled request parameters for selecting billing data.

If no Stripe customer exists, return a sensible FREE/uninitialized response rather than leaking or querying unrelated customers.

If a customer exists but has no subscription, return `subscription: null` and the appropriate current plan (normally FREE, consistent with the existing application's billing semantics).

Invoices should return an empty array when there are none.

## Authorization for GET

Use the same billing authorization boundary as the portal endpoint unless the existing product design clearly indicates otherwise:

- `SUPER_ADMIN` → allowed
- `ADMIN` → allowed
- `MANAGER` → forbidden
- `USER` → forbidden

---

# DTO requirements

Create dedicated response DTOs rather than returning JPA entities directly.

For example:

- `BillingSummaryResponse`
- `SubscriptionResponse`
- `InvoiceResponse`
- `CreatePortalSessionResponse`

Use records if that matches the existing project style.

Do not expose internal database IDs unless the existing API convention explicitly requires them.

Prefer Stripe/public billing identifiers only where the frontend actually needs them.

---

# Repository/service design

Keep controller logic thin.

Prefer:

```text
BillingController
    ↓
BillingService
    ↓
TenantRegistryService / repositories / Stripe client
```

rather than putting all lookup and transformation logic inside the controller.

Extend `BillingService` where appropriate.

Do not create a second `BillingService`.

Keep Stripe portal-session creation isolated from the local billing-summary lookup so the read endpoint does not accidentally invoke Stripe.

---

# Tests required for billing

Add tests using the project's existing testing style.

## Portal endpoint

Cover:

1. `ADMIN` can create a portal session when the tenant has a Stripe customer.
2. `SUPER_ADMIN` can create a portal session.
3. `MANAGER` receives `403`.
4. `USER` receives `403`.
5. Tenant without `stripe_customer_id` receives a clean client error.
6. The Stripe portal session is created for the **current tenant's** customer ID.
7. Stripe failure is translated into the project's expected upstream error response.
8. No Stripe customer ID can be supplied by the client to override tenant resolution.

Mock the Stripe API/client at the appropriate boundary. Do not make real Stripe network calls in tests.

## GET billing summary

Cover:

1. `ADMIN` can retrieve billing information.
2. `SUPER_ADMIN` can retrieve billing information.
3. `MANAGER` receives `403`.
4. `USER` receives `403`.
5. Correct plan is returned from the tenant's current billing state.
6. Correct subscription data is returned.
7. Correct invoices are returned.
8. Tenant with no Stripe customer gets the expected FREE/uninitialized response.
9. Tenant with a customer but no subscription gets `subscription: null`.
10. No cross-tenant billing data can be returned.
11. Existing webhook-created subscription/invoice records are correctly represented by the API.

---

# Security requirements

Do not weaken any existing security controls.

Specifically:

- Do not trust tenant IDs from the request.
- Do not trust Stripe customer IDs from the request.
- Do not allow ADMIN → SUPER_ADMIN invitation.
- Do not expose Stripe secret keys.
- Do not return raw Stripe exception messages if they could expose sensitive internals.
- Do not bypass Spring Security for convenience.
- Do not disable or alter the existing role hierarchy.
- Do not return JPA entities directly.
- Do not introduce a generic "current user can access anything in tenant" shortcut that bypasses role checks.

---

# Backward compatibility

Do not break:

- signup
- login
- JWT authentication
- refresh tokens
- tenant resolution
- role hierarchy
- invite acceptance
- existing invitation flow
- Stripe checkout
- Stripe webhooks
- subscription synchronization
- invoice synchronization
- existing frontend APIs

Do not rename existing endpoints unless absolutely necessary.

---

# Validation / acceptance criteria

Before considering the work complete:

### Security

- [ ] ADMIN cannot invite SUPER_ADMIN.
- [ ] SUPER_ADMIN can still invite SUPER_ADMIN.
- [ ] Existing RBAC hierarchy remains intact.
- [ ] Billing endpoints cannot access another tenant's customer/subscription/invoices.
- [ ] Client cannot choose the Stripe customer ID.

### Billing

- [ ] `POST /api/billing/portal-session` works for authorized tenant admins.
- [ ] Portal session uses the current tenant's stored Stripe customer.
- [ ] `GET /api/billing` (or the chosen equivalent) returns plan, subscription, and invoices.
- [ ] Data comes from existing local billing persistence.
- [ ] Empty/uninitialized billing states are handled cleanly.
- [ ] Stripe errors are handled consistently.

### Tests

- [ ] New RBAC escalation tests pass.
- [ ] New portal-session tests pass.
- [ ] New billing-summary tests pass.
- [ ] Existing test suite still passes.

### Code quality

- [ ] Reuse existing services/repositories/configuration.
- [ ] No duplicated billing logic.
- [ ] No unnecessary database migration.
- [ ] No unrelated refactors.
- [ ] DTOs are used for API responses.
- [ ] Controller remains thin.
- [ ] Code follows existing project naming/style.

---

# Final execution instructions

1. Inspect the repository thoroughly before editing.
2. Identify the exact current implementation and existing test helpers.
3. Implement Task 1 first.
4. Run the relevant RBAC/invitation tests.
5. Implement Task 2.
6. Run the relevant billing tests.
7. Run the complete backend test suite.
8. Fix any regressions caused by the changes.
9. Review the final diff for unnecessary changes.
10. Summarize:
   - files changed
   - security fix
   - new billing endpoints
   - response shapes
   - tests added/updated
   - commands/tests run
   - any configuration/environment variable added

**Do not stop at analysis. Make the code changes and tests.**
