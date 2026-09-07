# Copilot instructions for `saas-platform`

## Repository shape

This is a two-part application:

- `backend/` is a Spring Boot 4.1 application targeting Java 25. It exposes the REST API, authentication, tenant routing, persistence, Flyway migrations, and Redis-backed refresh-token storage.
- `frontend/` is a React 19 + TypeScript application built with Vite 8. It is currently a small client that checks backend health.
- `docker-compose.yml` provides the local PostgreSQL 16 and Redis 7 dependencies.

There is no root build that coordinates both applications. Run backend commands from `backend/` and frontend commands from `frontend/`.

## Build, test, lint, and local services

Start the local dependencies before exercising the backend:

```bash
docker compose up -d
```

The local `application.yaml` expects PostgreSQL at `localhost:5434` (`saas_db`, user/password `saas`/`saas_dev_pw`) and Redis at `localhost:6380`. The backend listens on `http://localhost:8090`.

Backend commands, from `backend/`:

```bash
./mvnw -B clean verify       # compile and run the full Maven verification
./mvnw test                  # run the test suite
./mvnw -Dtest=BackendApplicationTests test
./mvnw -Dtest=BackendApplicationTests#contextLoads test
./mvnw spring-boot:run      # run the API locally
```

The backend CI workflow uses Java 25 and runs `./mvnw -B clean verify`. Its PostgreSQL and Redis service containers use the default container ports (`5432` and `6379`), so do not assume the local Docker Compose host-port mappings apply in CI; use the workflow/application configuration when changing integration-test setup.

Frontend commands, from `frontend/`:

```bash
npm ci
npm run dev
npm run build
npm run lint
npm run preview
```

There is currently no frontend test script or frontend test runner configured, so there is no single-frontend-test command. Frontend CI installs with `npm ci` and runs `npm run build`; lint is available locally but is not part of that workflow.

## Architecture

### Backend request and tenant flow

The backend uses schema-per-tenant PostgreSQL multi-tenancy:

1. Flyway's global migration location (`db/migration/global`) creates `public.tenant_registry`.
2. Tenant provisioning runs the tenant migration location (`db/migration/tenant`) against a validated schema name. Tenant migrations create the tenant-local `users`, `customers`, invoices, and subscriptions tables.
3. `TenantContext` stores the current schema in a request-thread `ThreadLocal`.
4. `TenantIdentifierResolver` supplies the current schema to Hibernate, falling back to `public`.
5. `TenantConnectionProvider` obtains a pooled connection and calls `Connection.setSchema(...)`; it resets the schema before releasing the connection.
6. `TenantRegistryService` deliberately uses `public.tenant_registry` through `JdbcTemplate`, so registry lookups are not tenant-routed.

When adding tenant-scoped repositories or services, preserve this distinction: registry access belongs to `public`, while JPA access must occur with the correct tenant context set. Code that sets the context directly must clear it in a `finally` block.

### Authentication and security

`POST /api/auth/login` first resolves an active tenant from the global registry, sets that tenant schema, checks the tenant-local user with BCrypt, issues access/refresh JWTs, and clears the context. `JwtService` signs RS256 tokens from the configured classpath RSA key pair and stores refresh-token JTIs in Redis.

`JwtAuthenticationFilter` validates bearer tokens, resolves the token's `tenant_id` through the global registry, sets the tenant schema, and creates authorities from the token's `role` claim. It clears `TenantContext` after every request. `SecurityConfig` is stateless: auth, OpenAPI, and CORS preflight routes are public; all other routes require authentication.

The committed private JWT key is referenced by `application.yaml` and ignored by `.gitignore`; do not replace this with a checked-in secret or add new secrets to source control.

### Database migrations

Migration locations are intentionally separate:

- `backend/src/main/resources/db/migration/global/` changes the public tenant registry.
- `backend/src/main/resources/db/migration/tenant/` changes every tenant schema.

Use the next Flyway version in the relevant location and make tenant migrations safe for every provisioned schema. Do not edit an already-applied migration to change database behavior.

### Frontend/backend integration

The frontend has no configured Vite proxy and currently calls the health endpoint with a hardcoded URL in `frontend/src/App.tsx`. The backend's configured port is `8090`, while the current frontend URL uses `8081`; keep this mismatch in mind when debugging local health checks or changing the API base URL.

## Codebase-specific conventions

- Keep backend code organized by responsibility under `com.yourco.saas`: `auth`, `config`, `tenant`, and domain packages. DTOs live under the feature package's `dto` subpackage.
- Use Java records for small request/response/value shapes such as `LoginRequest`, `TokenResponse`, `TokenPair`, and `TenantRecord`.
- Persist enum values as strings with `@Enumerated(EnumType.STRING)` and keep Java enum values aligned with the SQL `CHECK` constraints in tenant migrations.
- Use Spring Data repositories for tenant-local JPA entities. Use `JdbcTemplate` with explicit `public.tenant_registry` qualification for the global registry.
- Validate tenant IDs and schema names before dynamic schema operations. Schema names use lowercase letters followed by lowercase letters, digits, or underscores; tenant IDs additionally permit hyphens.
- Preserve explicit cleanup around `TenantContext`; request filters and controller/service code must not leak a schema into a pooled thread.
- Authentication failures should remain indistinguishable (`Invalid credentials` for login failures), and invalid bearer tokens should leave the security context unauthenticated so Spring Security produces the configured 401 response.
- Frontend TypeScript follows the existing flat Vite structure (`App.tsx`, `main.tsx`, `App.css`, `index.css`) and the flat ESLint configuration in `frontend/eslint.config.js`. Keep imports and formatting consistent with the existing files.

## CI and quality configuration

- Backend changes are covered by `.github/workflows/backend-ci.yml`.
- Frontend changes are covered by `.github/workflows/frontend-ci.yml`.
- `qodana.yaml` and `.github/workflows/qodana_code_quality.yml` provide the repository's Qodana quality integration.
- Path filters mean backend and frontend workflows run independently based on changed paths.
