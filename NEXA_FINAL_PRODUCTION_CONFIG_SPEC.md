# Nexa — Final Production Configuration Specification
## Stripe Webhooks + Google OAuth + PostgreSQL/HikariCP Pooling

This phase addresses the three remaining production-configuration risks identified in the latest Nexa Production-Completion Report:

1. Stripe webhook endpoint and signing-secret synchronization
2. Google OAuth production origins and redirect URIs
3. HikariCP/PostgreSQL connection-pool sizing for growth

The existing report confirms these are the remaining production risks. Do not redesign working application architecture.

---

# 1. Stripe Webhook Production Configuration

## Goal

Ensure Stripe sends production webhook events to the actual Nexa backend and that the backend verifies them using the exact signing secret generated for that endpoint.

Production endpoint:

```text
https://<NEXA_DOMAIN>/api/webhooks/stripe
```

The actual production domain must be discovered from the deployment configuration before replacing `<NEXA_DOMAIN>`.

Do not invent a domain.

## Existing architecture

Preserve:

```text
Stripe
  ↓
HTTPS POST
  ↓
/api/webhooks/stripe
  ↓
Stripe signature verification
  ↓
Atomic event claim
  ↓
Tenant resolution
  ↓
Billing action
  ↓
Processed webhook record
  ↓
Subscription / invoice state
  ↓
Feature entitlements
```

The current implementation already uses atomic webhook event claiming. Do not replace it with a weaker check-then-act flow.

## Stripe Dashboard Configuration

In Stripe Dashboard, configure the production webhook endpoint:

```text
Endpoint:
https://<NEXA_DOMAIN>/api/webhooks/stripe
```

Use HTTPS.

Do not use:

```text
http://...
localhost
127.0.0.1
```

for the production endpoint.

## Events

Inspect the actual backend webhook controller and subscribe only to event types the application handles.

At minimum verify the application's current handlers for:

- Checkout/session completion
- Subscription lifecycle
- Invoice lifecycle

Do not subscribe to large numbers of unused events.

## Signing Secret

Stripe provides a unique signing secret for the webhook endpoint.

The production value must be configured as:

```text
STRIPE_WEBHOOK_SECRET=<production_endpoint_secret>
```

Do not reuse:

```text
whsec_... from local development
```

unless it is explicitly the secret for the exact production endpoint.

Do not put the signing secret in:

- React
- Vite environment variables
- browser code
- Git
- README
- Docker image
- logs

The secret belongs only in the backend runtime environment.

## Stripe Secret Key

Production:

```text
STRIPE_SECRET_KEY=sk_live_...
```

Do not put this in frontend code.

Local/test environments should continue using test credentials.

## Price IDs

Verify that production subscription plans use the correct production Stripe Price IDs.

Maintain:

```text
STARTER
PRO
ENTERPRISE
```

Do not accidentally use test-mode Price IDs in production.

If Price IDs are environment-specific, configure them through environment variables rather than hardcoding test values.

## Verification Checklist

After configuration:

```text
[ ] Production webhook endpoint exists
[ ] HTTPS works
[ ] Endpoint responds
[ ] Stripe signature verification succeeds
[ ] Correct STRIPE_WEBHOOK_SECRET configured
[ ] Production STRIPE_SECRET_KEY configured
[ ] Production Price IDs configured
[ ] Checkout completion updates subscription
[ ] Subscription events update plan/status
[ ] Invoice events update invoice records
[ ] Duplicate event does not execute billing twice
[ ] Failed transient processing can be retried
```

## Important

Do not test production credentials by printing them.

Use Stripe Dashboard's webhook delivery/test tooling and inspect application logs without exposing secrets.

---

# 2. Google OAuth Production Configuration

## Goal

Ensure Google authentication works from the real Nexa production domain without weakening OAuth security.

The current Google OAuth configuration must be inspected first to determine the exact callback/redirect endpoint used by the Spring Security configuration.

Do not invent the callback URL.

## Required values

Production JavaScript origin:

```text
https://<NEXA_DOMAIN>
```

Production backend OAuth callback:

```text
https://<NEXA_DOMAIN>/<ACTUAL_GOOGLE_CALLBACK_PATH>
```

The callback path must be taken from the existing Spring Security configuration.

Do not assume a callback path.

## Google Cloud Console

In:

```text
Google Cloud Console
→ Google Auth Platform
→ Clients
→ Web application OAuth client
```

configure the production domain.

Google distinguishes between:

### Authorized JavaScript origins

These identify the web origins from which browser-side OAuth requests may originate.

Example:

```text
https://nexa.example.com
```

### Authorized redirect URIs

These must exactly match the application's OAuth callback URI.

Example:

```text
https://nexa.example.com/login/oauth2/code/google
```

Only use the actual callback path found in the repository.

Google requires redirect URIs to exactly match configured authorized redirect URIs. HTTPS should be used for production.

## Local development

Keep local configuration separately:

```text
http://localhost:5173
```

and whatever local callback endpoint the current backend uses.

Do not replace local values with production values.

The application should support environment-specific OAuth configuration.

## Google Client ID / Secret

Production:

```text
GOOGLE_CLIENT_ID=<production-client-id>
GOOGLE_CLIENT_SECRET=<production-client-secret>
```

Do not commit the client secret.

The client ID is not a password, but still keep environment configuration centralized.

## Authorized Domains

The production domain should be owned/verified by the project.

The public Nexa landing page should exist on the production domain.

Production Google OAuth readiness also requires a publicly accessible home page and appropriate privacy/terms links when required by Google's verification policies.

## Verification Checklist

```text
[ ] Production domain is HTTPS
[ ] Domain is owned/verified
[ ] Authorized JavaScript origin configured
[ ] Exact OAuth redirect URI configured
[ ] Production Google Client ID configured
[ ] Production Client Secret configured securely
[ ] Local OAuth still works
[ ] Production Google login works
[ ] OAuth callback succeeds
[ ] Tenant resolution still occurs
[ ] Correct user/role is created or resolved
[ ] JWT access/refresh tokens are issued correctly
[ ] No OAuth secret reaches React
```

## Security

Do not add broad wildcard origins.

Do not add arbitrary domains simply to make OAuth work.

Do not disable state/nonce/security checks already provided by the OAuth framework.

---

# 3. HikariCP / PostgreSQL Connection Pooling

## Goal

Configure database connection pooling so Nexa can scale beyond a small local deployment without exhausting PostgreSQL connections.

Spring Boot exposes Hikari-specific configuration through:

```text
spring.datasource.hikari.*
```

The current datasource configuration must be inspected before changing values.

## Important architecture consideration

Nexa uses schema-per-tenant.

This does NOT mean every tenant should receive its own Hikari pool.

The application should continue using the existing datasource/routing architecture.

The target is:

```text
Application
     ↓
HikariCP
     ↓
PostgreSQL
     ↓
Tenant schema selection
```

not:

```text
Tenant A → Pool A
Tenant B → Pool B
Tenant C → Pool C
...
```

unless the existing architecture explicitly requires that.

## Initial production configuration

Do not blindly choose a huge pool.

Start with a conservative configurable baseline, for example:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: ${DB_MAX_POOL_SIZE:10}
      minimum-idle: ${DB_MIN_IDLE:2}
      connection-timeout: ${DB_CONNECTION_TIMEOUT_MS:30000}
      idle-timeout: ${DB_IDLE_TIMEOUT_MS:600000}
      max-lifetime: ${DB_MAX_LIFETIME_MS:1800000}
```

These values are a starting configuration, not a universal production recommendation.

Inspect the actual database hosting limits before finalizing them.

## Pool sizing rule

The important constraint is:

```text
Total possible DB connections
=
Hikari maximum pool size
×
number of backend instances
```

If the application has:

```text
3 backend instances
10 connections each
```

then the application may consume approximately:

```text
30 PostgreSQL connections
```

before accounting for administrative/other connections.

Therefore:

```text
PostgreSQL max_connections
>
application pool capacity
+
operational reserve
```

Do not set Hikari maximum-pool-size to a large value simply because the database supports many connections.

## Production scaling

As Nexa grows:

```text
Tenants
   ↓
Concurrent requests
   ↓
DB query concurrency
   ↓
Connection utilization
```

Measure before increasing the pool.

Useful metrics include:

```text
jdbc.connections.active
jdbc.connections.idle
jdbc.connections.max
jdbc.connections.min
hikaricp.*
```

Spring Boot provides datasource and Hikari metrics when metrics instrumentation is available.

## Recommended environment variables

Expose the tunable values:

```text
DB_MAX_POOL_SIZE=10
DB_MIN_IDLE=2
DB_CONNECTION_TIMEOUT_MS=30000
DB_IDLE_TIMEOUT_MS=600000
DB_MAX_LIFETIME_MS=1800000
```

Use the project's existing environment/property naming convention if different.

## Do not over-pool

Avoid:

```text
maximum-pool-size: 50
```

or:

```text
maximum-pool-size: 100
```

without evidence that the workload and PostgreSQL capacity require it.

A larger pool does not automatically mean higher throughput.

## PostgreSQL capacity review

Before finalizing production settings, determine:

```text
PostgreSQL max_connections
Current DB connections
Expected backend instance count
Expected concurrent request volume
Expected average DB query time
Connection pool utilization
```

Then calculate a safe pool budget.

Example:

```text
PostgreSQL capacity: 100
Reserved operational connections: 20
Application budget: 80

2 backend instances
→ approximately 40 max connections per instance
```

The exact production number must be based on the actual PostgreSQL provider and deployment topology.

## Connection pool monitoring

Expose/inspect:

```text
Active
Idle
Maximum
Minimum
Pending/waiting requests where available
Connection acquisition latency
```

If active connections remain close to maximum under normal workload, investigate query latency and pool sizing.

Do not immediately increase the pool.

---

# 4. Database Performance Checks

Before increasing pool size, inspect:

- Slow queries
- Missing indexes
- Long transactions
- N+1 queries
- Unclosed connections
- Excessive transaction duration
- Large result sets

The application already has indexes in important tenant tables. Preserve them and inspect query plans before adding unnecessary indexes.

---

# 5. Production Configuration Separation

Use clear environments:

```text
Development
Test
Production
```

Conceptually:

```text
application.yaml
application-test.yaml
application-prod.yaml
```

Do not hardcode production credentials.

Production should inject:

```text
STRIPE_SECRET_KEY
STRIPE_WEBHOOK_SECRET
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
DATABASE_URL
DATABASE_USER
DATABASE_PASSWORD
REDIS credentials
JWT keys
DB pool configuration
FRONTEND origin
```

Use the actual property names already present in the repository.

---

# 6. Verification Plan

## Stripe

Perform:

```text
Production/Test-mode checkout
        ↓
Stripe event
        ↓
Webhook endpoint
        ↓
Signature verification
        ↓
Atomic claim
        ↓
Billing update
        ↓
Subscription/feature update
```

Verify duplicate webhook delivery.

Verify retry behavior after a transient failure.

## Google

Perform:

```text
Production landing
        ↓
Login
        ↓
Continue with Google
        ↓
Google authorization
        ↓
Exact callback URI
        ↓
Backend authentication
        ↓
JWT
        ↓
/app
```

Verify no redirect URI mismatch.

## Database

Perform:

```text
Backend startup
        ↓
Hikari pool
        ↓
PostgreSQL
        ↓
Tenant resolution
        ↓
Normal CRUD
```

Then inspect pool metrics under concurrent requests.

---

# 7. Production Readiness Checklist

## Stripe

```text
[ ] Production endpoint configured
[ ] HTTPS
[ ] Correct webhook path
[ ] Correct signing secret
[ ] Correct live/test mode
[ ] Correct Price IDs
[ ] Duplicate event protection
[ ] Retry behavior verified
```

## Google OAuth

```text
[ ] Production domain
[ ] HTTPS
[ ] Authorized JavaScript origin
[ ] Exact redirect URI
[ ] Production client ID
[ ] Production secret
[ ] Authorized domain
[ ] OAuth login tested
```

## Database

```text
[ ] Production PostgreSQL capacity known
[ ] Hikari pool configured
[ ] Pool size environment configurable
[ ] Multiple backend instances considered
[ ] Operational DB connection reserve considered
[ ] Hikari metrics available
[ ] Slow-query review completed
[ ] No connection leaks
```

---

# 8. Coding Agent Prompt

```text
Read PROJECT_SPEC.md first.

Then read the latest Nexa production-completion report and this specification.

We are implementing ONLY the final production configuration tasks:

1. Stripe webhook endpoint/signing-secret synchronization
2. Google OAuth production origins/redirect URIs
3. HikariCP/PostgreSQL connection-pool sizing

Do not redesign the product.

Do not modify the landing page.

Do not replace working authentication, billing, tenant isolation, RBAC,
or subscription architecture.

FIRST: inspect the repository.

For Stripe inspect:
- StripeWebhookController
- Stripe configuration
- application.yaml/properties
- environment variable names
- Stripe Price configuration
- existing webhook tests
- Docker environment

For Google inspect:
- Spring Security configuration
- OAuth2 client configuration
- LoginPage/frontend Google integration
- application configuration
- callback/redirect paths
- environment variable names

For PostgreSQL/Hikari inspect:
- datasource configuration
- TenantRoutingDataSource
- TenantIdentifierResolver
- Hikari configuration
- Docker Compose
- production environment configuration
- Testcontainers configuration
- Actuator/metrics configuration

REPORT THE CURRENT CONFIGURATION BEFORE CHANGING IT.

Then implement only the necessary code/configuration changes.

STRIPE:
- Keep signature verification.
- Keep atomic webhook event claiming.
- Configure production endpoint through environment/deployment configuration.
- Do not commit secrets.
- Keep test and production credentials separate.
- Verify the exact event types handled by the backend.
- Verify Price IDs are environment-specific if necessary.

GOOGLE:
- Determine the exact callback URI from the existing Spring Security setup.
- Do not invent a callback path.
- Make origin/redirect configuration environment-specific.
- Do not use wildcard production origins.
- Keep local development working.
- Do not expose the Google client secret to React.

HIKARICP:
- Inspect current datasource architecture before modifying it.
- Preserve schema-per-tenant routing.
- Do not create one Hikari pool per tenant.
- Make pool parameters configurable through environment variables.
- Start with conservative production values.
- Consider total backend instance count and PostgreSQL max_connections.
- Expose/verify Hikari datasource metrics if the existing Actuator setup supports them.
- Do not arbitrarily increase pool size.

After implementation run:

./mvnw test

npm run lint

npm run build

docker compose config

If possible, run the complete Docker stack and verify health.

Then report:

1. Existing configuration found
2. Stripe changes
3. Google OAuth changes
4. Hikari/PostgreSQL changes
5. Environment variables added/changed
6. Tests
7. Build
8. Docker validation
9. Any manual production-console steps still required

IMPORTANT:
Do not claim Stripe Dashboard, Google Cloud Console, or production DNS
configuration is complete unless it was actually configured externally.

Clearly separate:
- code/configuration completed in the repository
- manual production-console steps that I must perform
```

---

# 9. Manual Production Actions

The coding agent can prepare the repository, but these three external actions require access to the relevant production accounts.

### Stripe

Configure the production webhook endpoint:

```text
https://YOUR_DOMAIN/api/webhooks/stripe
```

and copy that endpoint's signing secret into the production secret store.

### Google Cloud

Configure:

```text
Authorized JavaScript origin:
https://YOUR_DOMAIN
```

and the exact callback URI discovered from the backend.

Google requires production OAuth redirect URIs to match exactly, and production web OAuth should use HTTPS. citeturn0search1turn0search5

### PostgreSQL

Find the actual provider's connection limit and deployment topology, then size Hikari based on the number of backend instances and available database connections rather than using an arbitrarily large pool. Spring Boot exposes Hikari configuration under `spring.datasource.hikari.*`, including `maximum-pool-size`, `minimum-idle`, and connection timeouts. citeturn0search2turn0search3

Spring Boot also exposes datasource/Hikari metrics that can be used to observe active, idle, maximum, and minimum connections. citeturn0search9
