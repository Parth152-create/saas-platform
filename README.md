# Nexa SaaS Multi-Tenant Platform

Nexa is an enterprise multi-tenant SaaS application built with a schema-per-tenant architecture on PostgreSQL, Spring Boot, and React.

## Billing Architecture & Subscription Lifecycle

### Core Concept: Effective Subscription Resolution

> **Important Distinction:**  
> Stripe subscription history may contain multiple records per customer, but Nexa resolves **one effective subscription** for tenant entitlement.

In Stripe, multiple subscription records can exist for a customer over time due to upgrades, plan transitions, test checkout sessions, or cancellation lifecycles. Rather than assuming `customer_id` is unique in the local database or deleting historical records, Nexa preserves complete subscription and invoice history while deterministically selecting a single **Current Subscription** to govern the tenant's plan tier and feature entitlements.

### Subscription Lifecycle & Status Handling

Subscriptions transition through explicit states represented by `SubscriptionStatus`:

| Status | Entitled? | Business Semantics |
|---|:---:|---|
| `ACTIVE` | Yes | Paid subscription in good standing. Grants paid plan tier features (e.g. `PRO`, `ENTERPRISE`). |
| `TRIALING` | Yes | Active free trial. Grants paid plan tier features during trial duration. |
| `PAST_DUE` | Yes | Invoice payment failed; Stripe is actively retrying (dunning grace period). Entitlements remain granted until retries are exhausted. |
| `PAUSED` | No | Subscription paused. Does not grant paid features; tenant falls back to `FREE` / `STARTER`. |
| `UNPAID` | No | Payment retries exhausted. Subscription suspended. Effective plan is `FREE`. |
| `INCOMPLETE` | No | Initial checkout payment incomplete (e.g. pending 3DS or failed initial charge). Does not grant paid features. |
| `INCOMPLETE_EXPIRED` | No | Incomplete checkout timed out after 23 hours. Terminal dead state; does not grant paid features. |
| `CANCELED` | No | Subscription terminated or deleted. Effective plan is `FREE`. |

#### Cancellation at Period End vs Immediate Cancellation

A subscription scheduled to cancel at the end of the billing period has `cancel_at_period_end = true` while retaining `status = ACTIVE`. Nexa preserves this distinction:
- The subscription remains active and entitled through its `current_period_end`.
- The billing UI indicates "(Set to cancel at end of period)".
- The subscription is only transitioned to `status = CANCELED` when Stripe delivers `customer.subscription.deleted` at the period expiration.

### Deterministic Current Subscription Selection

When evaluating subscriptions for a customer:
1. **Entitlement Priority:** Active/entitled subscriptions (`ACTIVE`, `TRIALING`, `PAST_DUE`) always take precedence over non-entitled subscriptions (`CANCELED`, `UNPAID`, `INCOMPLETE`, etc.).
2. **Recency Determination:** Among candidate subscriptions of the same entitlement class, Nexa deterministically picks the newest subscription by comparing:
   - `current_period_start` (descending, nulls last)
   - `created_at` (descending, nulls last)
   - `updated_at` (descending, nulls last)
   - `id` (descending, primary key tie-breaker)
3. **Historical Fallback:** If all subscriptions are canceled or non-entitled, the most recent interaction is returned to display the canceled state in the billing summary, while the effective plan resolves to `FREE` (and normalized to `STARTER` in the entitlement matrix).

### Stripe Webhook Processing & Reconciliation

All Stripe webhook events (`customer.subscription.created`, `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.paid`, `invoice.payment_failed`) are processed through transactional tenant-scoped handlers:

1. **Idempotency & Atomicity:** Events are verified against Stripe signatures and claimed atomically in `processed_webhook_events`. Duplicates are safely acknowledged without re-executing state mutations.
2. **Centralized Reconciliation:** Webhooks update the targeted Stripe subscription record in the tenant's schema and invoke `reconcileCustomerSubscriptions()`.
3. **Out-of-Order Webhook Protection:** Tenant plan synchronization is derived from the globally reconciled current subscription rather than whichever webhook arrived last. Stale webhooks for older subscriptions cannot downgrade or overwrite a newer active subscription.
4. **Cancellation Safety:** When a subscription is deleted (`customer.subscription.deleted`), the system recalculates the effective plan from any remaining active subscriptions. The tenant only transitions to `FREE` when no valid paid subscriptions remain.

### Checkout & Portal Session Management

To prevent accidental subscription duplication:
- `POST /api/billing/checkout-session` rejects requests if the tenant already possesses an active/entitled subscription (`400 Bad Request`), advising the administrator to use the Stripe Customer Portal for plan modifications.
- Upgrades, downgrades, payment method updates, and cancellations for active subscriptions are routed through `POST /api/billing/portal-session` into the Stripe Customer Portal.
- Once a subscription is canceled, checkout session creation is re-enabled for the tenant.

### Feature Entitlement Resolution

Both `/api/billing` (Billing Summary) and `/api/billing/entitlements` share the exact same resolution mechanism:
- If an entitled subscription exists, its `plan_tier` (`PRO`, `ENTERPRISE`) is the effective plan.
- If no entitled subscription exists, the effective plan is `FREE`.
- In the entitlement response, `FREE` is normalized to `STARTER` for client presentation, mapping to standard starter features.
- Method-level security annotations (`@RequiresFeature`) enforce tenant entitlements consistently via `FeatureEntitlementAspect`.
