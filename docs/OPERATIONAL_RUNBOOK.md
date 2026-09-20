# Nexa — Operational Runbook & Production Readiness Guide

This runbook outlines operational procedures and configuration requirements for the Nexa Multi-Tenant SaaS platform.

---

## 1. Database Backup & Restore Procedures

Nexa operates on a **schema-per-tenant PostgreSQL architecture**:
- **`public` schema**: Contains global `tenant_registry` and tenant metadata.
- **`tenant_<id>` schemas**: Contains tenant-isolated data (users, audit logs, billing customers/subscriptions, HRM entities, projects, tasks, collaboration channels, direct messages, notifications, and calendar events).

A complete database backup must capture both the `public` schema and all `tenant_*` schemas.

### 1.1 Automated Backup (`scripts/db-backup.sh`)

The backup script uses PostgreSQL's custom archive format (`-F c`), which preserves schema structure, object ownership, large objects, and enables parallel, selective, or complete restores.

```bash
# Basic usage (defaults to backups/nexa_<db>_<timestamp>.dump)
./scripts/db-backup.sh

# Specify custom destination
./scripts/db-backup.sh /path/to/my_backup.dump
```

**Environment Variables:**
- `POSTGRES_USER` (default: `saas`)
- `POSTGRES_DB` (default: `saas_db`)
- `POSTGRES_HOST` (default: `localhost`)
- `POSTGRES_PORT` (default: `5434`)
- `BACKUP_DIR` (default: `./backups`)

When running inside Docker Compose, the script automatically executes `pg_dump` via `docker compose exec -T postgres pg_dump`.

### 1.2 Database Restore (`scripts/db-restore.sh`)

> [!WARNING]
> Restoring a dump will overwrite existing records in matching schemas. By default, the script requires typing `YES` or passing the `--confirm` flag.

```bash
# Interactive restore (will prompt for 'YES')
./scripts/db-restore.sh backups/nexa_saas_db_20260920_170000.dump

# Non-interactive / script-driven restore
./scripts/db-restore.sh backups/nexa_saas_db_20260920_170000.dump --confirm
```

---

## 2. Redis Reliability & Persistence

Redis is utilized for:
- Refresh token state tracking (`refresh:<jti>`)
- User refresh token set tracking (`user_refresh:<userId>`)
- Immediate session revocation / user deactivation state (`user_disabled:<userId>`)
- Rate limiting request counters

### Persistence Configuration:
- Docker configuration runs Redis with **Append-Only File (AOF)** persistence enabled:
  `command: ["redis-server", "--appendonly", "yes"]`
- Redis data directory `/data` is mounted to a named Docker volume (`redisdata`).
- When containers are stopped or recreated, token revocation state and user deactivations persist across container lifecycles.

---

## 3. HikariCP Database Connection Pool Sizing

HikariCP pool parameters are externalized through environment variables:
- `DB_MAX_POOL_SIZE` (default: `10`)
- `DB_MIN_IDLE` (default: `2`)
- `DB_CONNECTION_TIMEOUT_MS` (default: `30000` ms)
- `DB_IDLE_TIMEOUT_MS` (default: `600000` ms)
- `DB_MAX_LIFETIME_MS` (default: `1800000` ms)

### Sizing Formula:
$$\text{Total Connections} = (\text{DB\_MAX\_POOL\_SIZE} \times \text{Backend Replicas}) + \text{Operational Reserve}$$

Ensure that:
$$\text{Total Connections} < \text{PostgreSQL } \texttt{max\_connections}$$

**Example:**
With 3 backend replicas and `DB_MAX_POOL_SIZE=10`:
$$3 \times 10 = 30 \text{ connections}$$
This leaves ample headroom on PostgreSQL's default `max_connections = 100`.

---

## 4. CORS & WebSocket Origin Security

### HTTP CORS:
- Controlled by `CORS_ALLOWED_ORIGINS` in `.env`.
- By default, local development patterns (`http://localhost:[*]`, `http://127.0.0.1:[*]`) are permitted.
- Allowed methods are explicit: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`.
- Allowed headers are explicit: `Authorization`, `Content-Type`, `Accept`, `Origin`, `X-Requested-With`, `X-Request-ID`, `X-Correlation-ID`, `X-Tenant-ID`, `Idempotency-Key`, `Cache-Control`, `If-Match`, `If-None-Match`.
- Preflight requests from unauthorized origins return `403 Forbidden`.

### WebSocket Origins:
- Controlled by `WEBSOCKET_ALLOWED_ORIGINS` in `.env`.
- Local development patterns (`http://localhost:[*]`, `http://127.0.0.1:[*]`) are allowed by default.
- WebSocket handshake requests (`GET /ws`) from unauthorized origins are rejected with `403 Forbidden`.
- In production, set `WEBSOCKET_ALLOWED_ORIGINS=https://app.yourdomain.com`.

---

## 5. Logging, MDC, and Observability

- **Correlation ID**: Every HTTP request is assigned an `X-Request-ID` correlation ID via `CorrelationIdFilter`, injected into SLF4J `MDC` under key `requestId`.
- **Tenant Context in MDC**: On authenticated requests, `JwtAuthenticationFilter` injects `tenantId` and `userId` into `MDC`, which are cleared in `finally` alongside `TenantContext.clear()`.
- **Sensitive Data Filtering**: Passwords, refresh tokens, JWT strings, Google OAuth tokens, and Stripe secrets are strictly prohibited from log statements.
- **Actuator Protection**: Only `health`, `info`, and `metrics` endpoints are exposed. Sensitive endpoints (`/actuator/env`, `/actuator/beans`, `/actuator/threaddump`) are unexposed and return `401 Unauthorized` or `404 Not Found`.

---

## 6. User Lifecycle & Reactivation

- **Deactivation**: `POST /api/users/{userId}/deactivate` or `DELETE /api/users/{userId}`:
  - Sets user status to `DISABLED`.
  - Revokes all active refresh tokens in Redis.
  - Sets `user_disabled:{userId}` in Redis.
  - Logs `USER_DEACTIVATE` audit event.
- **Reactivation**: `POST /api/users/{userId}/reactivate`:
  - Enforces RBAC: `ADMIN` or `SUPER_ADMIN`.
  - Hierarchy protection: `ADMIN` cannot reactivate an `ADMIN` (only `SUPER_ADMIN` can).
  - Protected `SUPER_ADMIN` users cannot be modified.
  - Restores status to `ACTIVE`.
  - Deletes `user_disabled:{userId}` from Redis.
  - Logs `USER_REACTIVATE` audit event.
  - Frontend provides full confirmation modal with instant feedback.
