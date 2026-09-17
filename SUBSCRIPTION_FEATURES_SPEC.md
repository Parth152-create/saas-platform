# Subscription, Billing & Feature Entitlements Specification

## Objective

Implement a complete subscription-driven feature entitlement system.

Every business/tenant receives features according to the subscription it purchases.

Core flow:

Stripe Subscription
→ Tenant Subscription
→ Tenant Plan
→ Feature Entitlements
→ Backend Authorization
→ Frontend Feature Visibility

Subscription belongs to the business/tenant, not an individual user.

---

## 1. Business-specific subscriptions

Support multiple plans, initially:

- STARTER
- PRO
- ENTERPRISE

Example:

Tenant A → STARTER
Tenant B → PRO
Tenant C → ENTERPRISE

When a tenant's subscription changes, its available features must change accordingly.

Inspect the existing backend and Stripe configuration before finalizing plan names and price mappings. Do not hardcode Stripe price IDs in the frontend.

---

## 2. Feature entitlements

Create a centralized feature entitlement model based on the actual modules in the repository.

Possible features:

- EMPLOYEE_MANAGEMENT
- TEAM_MANAGEMENT
- PROJECT_MANAGEMENT
- CLAIMS
- TASK_MANAGEMENT
- TIME_TRACKING
- ATTENDANCE
- LEAVE_MANAGEMENT
- WORK_SCHEDULES
- DOCUMENTS
- BASIC_REPORTS
- ADVANCED_REPORTS
- ADVANCED_ANALYTICS
- ADVANCED_HRM
- CUSTOM_WORKFLOWS
- ADVANCED_INTEGRATIONS

Do not blindly implement features that do not exist in the product.

---

## 3. Plan → feature mapping

Use a centralized mapping.

Conceptual example:

| Feature | Starter | Pro | Enterprise |
|---|---:|---:|---:|
| Employee Management | ✓ | ✓ | ✓ |
| Teams & Departments | ✓ | ✓ | ✓ |
| Projects | ✓ | ✓ | ✓ |
| Tasks | ✓ | ✓ | ✓ |
| Claims | ✓ | ✓ | ✓ |
| Time Tracking | ✓ | ✓ | ✓ |
| Attendance | ✓ | ✓ | ✓ |
| Leave Management | ✓ | ✓ | ✓ |
| Work Schedules | ✓ | ✓ | ✓ |
| Documents | ✓ | ✓ | ✓ |
| Basic Reports | ✓ | ✓ | ✓ |
| Advanced Reports | — | ✓ | ✓ |
| Advanced Analytics | — | ✓ | ✓ |
| Advanced HRM | — | ✓ | ✓ |
| Custom Workflows | — | — | ✓ |
| Advanced Integrations | — | — | ✓ |

The actual matrix must be determined from the existing product.

---

## 4. Database architecture

Do NOT create separate databases for different subscription tiers.

Keep the existing schema-per-tenant architecture.

Tenant schema = data isolation.

Subscription plan = feature access.

Example:

SaaS Platform
├── Tenant A → Schema A → PRO
├── Tenant B → Schema B → STARTER
└── Tenant C → Schema C → ENTERPRISE

"Show DB features according to subscription tier" means the application must expose only capabilities the tenant is entitled to use. It does not mean creating separate databases for each plan.

---

## 5. Backend authorization

The backend is the source of truth.

Request
→ JWT authentication
→ Tenant resolution
→ RBAC authorization
→ Subscription feature authorization
→ Tenant data access

For a protected feature the backend must determine:

1. Valid JWT?
2. Current tenant?
3. Current user role?
4. Current tenant subscription?
5. Current plan?
6. Does the plan include the requested feature?

If not entitled, reject the request with the appropriate authorization response.

The frontend must never be the security boundary.

---

## 6. Feature entitlement API

Expose a backend endpoint following existing project conventions.

Conceptual response:

```json
{
  "plan": "PRO",
  "status": "ACTIVE",
  "features": [
    "EMPLOYEE_MANAGEMENT",
    "TEAM_MANAGEMENT",
    "PROJECT_MANAGEMENT",
    "TIME_TRACKING",
    "ADVANCED_REPORTS",
    "ADVANCED_ANALYTICS"
  ]
}
```

Use existing DTO/package conventions.

---

## 7. Frontend entitlement handling

Create a centralized entitlement mechanism.

Conceptual usage:

```ts
hasFeature("ADVANCED_ANALYTICS")
hasFeature("CUSTOM_WORKFLOWS")
hasFeature("ADVANCED_HRM")
```

Do not scatter:

```ts
if (plan === "PRO")
```

throughout React.

The frontend consumes feature entitlements returned by the backend.

---

## 8. Settings → Subscription

Add:

Settings → Subscription

The page should show:

- Current plan
- Subscription status
- Billing interval
- Renewal/end date when available
- Feature access
- Billing information
- Invoice history
- Manage Subscription

Example:

```text
Subscription

Current Plan
PRO

Status
Active

Billing
Monthly

[ Manage Subscription ]
```

Use the existing backend Stripe Customer Portal flow.

Do not implement Stripe Customer Portal directly in React.

---

## 9. Feature Access section

The Subscription page must explain what the tenant's plan provides.

Example:

```text
Feature Access

Your subscription determines which platform
capabilities are available to your organization.

✓ Employee Management
✓ Projects
✓ Time Tracking
✓ Advanced Reports
✓ Advanced Analytics

🔒 Custom Workflows
Enterprise plan required
```

The current tenant's feature list must come from the backend.

Do not hardcode it.

---

## 10. Explain feature handling services

Add an informational section:

### How feature access works

Your organization's subscription determines which platform features are available to your team.

Each subscription plan is associated with a defined set of feature entitlements. When your subscription changes, the platform updates the capabilities available to your organization.

Feature access is enforced by the platform's backend and reflected throughout the application.

Show:

```text
Subscription
      ↓
Plan
      ↓
Feature Entitlements
      ↓
Backend Authorization
      ↓
Available Features
```

Keep this understandable for normal business users.

---

## 11. Navigation and feature visibility

Sidebar/navigation should reflect feature entitlements.

For unavailable features either:

- Hide them, or
- Show them as locked with an upgrade prompt.

Use the approach consistent with the existing UX.

Premium features should clearly indicate the required subscription.

---

## 12. Invoice & payment history

Enable billing/invoice history using the existing backend.

Display:

- Invoice number
- Date
- Amount
- Currency
- Status
- Payment status
- Invoice action where supported

Use real backend data.

Never create fake invoice data.

---

## 13. Stripe responsibilities

Backend responsibilities:

- Stripe Checkout
- Stripe Customer Portal
- Webhook verification
- Subscription synchronization
- Invoice synchronization
- Tenant subscription state
- Plan determination

Frontend responsibilities:

- Display subscription
- Start checkout through backend
- Open customer portal through backend
- Display invoices
- Display feature entitlements
- Provide upgrade messaging

Never expose Stripe secret keys in the frontend.

The frontend must not determine authoritative subscription state.

---

## 14. Subscription changes

The desired flow is:

Stripe subscription change
→ Stripe webhook
→ Backend
→ Tenant subscription updated
→ Plan updated
→ Feature entitlements updated
→ Frontend receives updated state

Do not assume a checkout result means a specific plan until the backend confirms it.

---

## 15. RBAC + subscription

These are separate authorization dimensions.

RBAC asks:

"Is this user allowed to perform this action?"

Subscription entitlement asks:

"Does this tenant's plan include this feature?"

Both can be required.

Example:

ADMIN + PRO
→ Pro feature exists and user may administer it if RBAC permits.

USER + PRO
→ Pro feature exists, but the user may not have administrative permission.

ADMIN + STARTER
→ Admin permission does not grant access to a Pro-only feature.

Do not replace RBAC with subscription checks.

---

## 16. Security

Never trust frontend-supplied:

- tenant_id
- plan
- role
- subscription status
- feature list

The backend must determine all authoritative values.

A client must not bypass restrictions by changing:

- localStorage
- React state
- frontend JavaScript
- request bodies
- API payloads

---

## 17. Tests

Add tests for:

1. Starter tenant receives Starter features.
2. Pro tenant receives Pro features.
3. Enterprise tenant receives Enterprise features.
4. Starter cannot access Pro-only APIs.
5. Pro can access Pro-only APIs.
6. Enterprise can access Enterprise-only APIs.
7. Changing subscription changes entitlements.
8. One tenant cannot use another tenant's subscription.
9. Frontend values cannot bypass backend authorization.
10. Existing RBAC remains functional.
11. Existing cross-tenant isolation remains functional.
12. Existing Stripe webhook behavior remains functional.

Run the complete backend test suite.

---

## 18. Frontend states

Support:

- Loading
- Error
- No subscription
- Active subscription
- Past due/inactive subscription
- No invoices
- Invoices available
- Included feature
- Locked feature

Never leave a blank screen.

---

## 19. UI/UX

Match the existing enterprise SaaS design.

Use:

- Clean cards
- Subtle borders
- Restrained shadows
- Blue primary actions
- Semantic status indicators
- Responsive layout
- Dark/light theme support
- Existing typography and spacing
- Accessible controls

Avoid:

- Excessive gradients
- Excessive glassmorphism
- Fake metrics
- Fake billing data
- Unnecessary animations

---

## 20. Existing architecture must remain intact

Do not break:

- Schema-per-tenant isolation
- JWT authentication
- Refresh token rotation
- RBAC hierarchy
- Stripe webhook verification
- Processed webhook event handling
- Audit logging
- Tenant registry
- Existing billing APIs
- Existing tests

Make the smallest architectural changes necessary.

---

## 21. Acceptance criteria

The implementation is complete when:

1. Subscription appears under Settings.
2. Current tenant subscription comes from the backend.
3. Current plan is displayed.
4. Subscription status is displayed.
5. Billing information is displayed.
6. Invoice history uses real backend data.
7. Stripe Customer Portal opens through the backend.
8. Tenant feature entitlements are available through the backend.
9. Frontend visibility uses entitlements, not hardcoded plans.
10. Backend APIs enforce feature entitlements.
11. Starter cannot directly access Pro-only APIs.
12. Pro can access Pro features.
13. Enterprise can access Enterprise features.
14. Changing subscription changes feature entitlements.
15. Existing RBAC continues working.
16. Existing tenant isolation continues working.
17. No fake billing data is introduced.
18. No Stripe secrets are exposed.
19. Existing tests continue passing.
20. New entitlement tests are added.
21. Frontend handles loading/error/empty states.

---

## Final architecture

```text
                         STRIPE
                            │
                            ↓
                     SUBSCRIPTION
                            │
                            ↓
                          PLAN
                            │
                            ↓
                  FEATURE ENTITLEMENTS
                            │
                  ┌─────────┴─────────┐
                  ↓                   ↓
           BACKEND AUTHORIZATION   FRONTEND UI
                  │                   │
                  ↓                   ↓
             API / DATA         NAVIGATION /
                ACCESS             FEATURES
                  │
                  ↓
             TENANT SCHEMA
```

The subscription tier controls product capabilities while the existing tenant schema continues to provide data isolation and RBAC continues to control user permissions.
