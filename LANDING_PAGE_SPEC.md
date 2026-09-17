# SaaS Platform — Public Landing Page Specification

## Objective

Build a premium public marketing landing page for the existing SaaS Platform — Enterprise HRM application.

The landing page is the public front door of the product. It must explain what the platform is, who it is for, what problems it solves, its major features, security architecture, subscription tiers, benefits, and how visitors can get started or log in.

Do not invent customers, testimonials, usage numbers, awards, certifications, integrations, or business claims that are not supported by the repository.

## Routing

Change public routing to:

- `/` → Public Landing Page
- `/login` → Login
- `/signup` → Signup
- `/accept-invite` → Accept Invite
- `/app` → Authenticated Dashboard
- `/app/*` → Authenticated Application

Desired flow:

```text
                         /
                         │
                  Public Landing
                         │
             ┌───────────┴───────────┐
             ↓                       ↓
      Get Started →              Login
             ↓                       ↓
          /signup                  /login
             │                       │
             └───────────┬───────────┘
                         ↓
                        /app
```

Authenticated users visiting `/` may be redirected to `/app` if consistent with the existing auth architecture. Do not break authenticated routes.

## Design Direction

Create a premium enterprise B2B SaaS experience consistent with the existing authenticated application.

Use:
- Dark-first enterprise SaaS aesthetic
- Clean professional layout
- Existing brand/theme/components where possible
- Blue primary actions
- Green included/success states
- Amber/orange paid/upgrade indicators
- Subtle borders and shadows
- Strong typography hierarchy
- Generous whitespace
- Responsive desktop/tablet/mobile layout
- Light/dark theme compatibility if already supported

Avoid:
- Generic startup templates
- Cartoon illustrations
- Excessive gradients
- Excessive glassmorphism
- Neon effects
- Huge 3D objects
- Stock photography
- Fake logos/testimonials
- Excessive floating or bouncing elements

The actual product UI should be the main visual element.

## Page Structure

```text
NAVBAR
   ↓
HERO
   ↓
PRODUCT PREVIEW
   ↓
PLATFORM OVERVIEW
   ↓
FEATURES
   ↓
SECURITY & MULTI-TENANCY
   ↓
SUBSCRIPTION TIERS
   ↓
HOW IT WORKS
   ↓
FINAL CTA
   ↓
FOOTER
```

User journey:

```text
What is this?
     ↓
What does it do?
     ↓
Can it solve my organization's problems?
     ↓
Is it secure?
     ↓
Which plan do I need?
     ↓
How do I start?
```

## Navbar

Desktop:

```text
┌──────────────────────────────────────────────────────────────┐
│ S SaaS Platform   Product  Features  Security  Pricing       │
│                                  Login   [Get Started →]      │
└──────────────────────────────────────────────────────────────┘
```

Links:
- Product
- Features
- Security
- Pricing
- Login → `/login`
- Get Started → `/signup`

On scroll, transition smoothly from transparent to a subtle translucent/blurred surface with border/shadow.

Mobile should use a compact menu.

## Hero

Suggested content direction:

Eyebrow:
`ENTERPRISE WORKFORCE & BUSINESS OPERATIONS`

Heading:
`Run your entire organization from one intelligent workspace.`

Supporting text:
`Manage people, projects, time, schedules, claims, and operations from one secure, multi-tenant platform.`

CTAs:

`[ Get Started → ]    [ Login ]`

Get Started → `/signup`
Login → `/login`

Hero entrance:

```text
Eyebrow → fade + translateY
Heading → fade + translateY
Description → fade + translateY
CTA → fade + translateY
Product preview → scale(0.94) + translateY(40px) + fade
```

Use staggered, fast enough animations.

## Product Preview

Show a large visualization based on the actual application UI.

Conceptual:

```text
┌─────────────────────────────────────────────────────┐
│ Dashboard                                    Search │
├─────────────────────────────────────────────────────┤
│                                                     │
│ Employees       Projects       Hours       Tasks   │
│     124             32          4,281        91    │
│                                                     │
│             ┌───────────────────────────┐           │
│             │       Activity Chart      │           │
│             └───────────────────────────┘           │
└─────────────────────────────────────────────────────┘
```

Prefer reusable existing dashboard components/screens.

Use sanitized demo data only; never expose real tenant/user data.

Optional micro-animations:
- KPI count-up once
- Chart reveal once
- Activity state transition once

Do not continuously animate the dashboard.

## Platform Overview

Heading:

`Everything your team needs. One connected workspace.`

Show major product areas:

```text
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ Workforce & HR   │ │ Projects & Tasks │ │ Time Tracking    │
│ Employees, teams │ │ Projects, tasks  │ │ Hours, schedules │
│ attendance, leave│ │ claims, workflow │ │ attendance       │
└──────────────────┘ └──────────────────┘ └──────────────────┘

┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ Reports &        │ │ Claims & Expenses │ │ Documents & Files│
│ Analytics        │ │ Reimbursements   │ │ Workforce records│
└──────────────────┘ └──────────────────┘ └──────────────────┘
```

Use consistent scroll-reveal animations.

## Problem → Solution

Heading:

`Your organization's work shouldn't live in disconnected systems.`

Problems:

```text
01  Fragmented workflows
    People, projects, time and HR processes live across different tools.

02  Limited visibility
    Managers lack one connected view of workforce and operational activity.

03  Administrative complexity
    Managing access, teams, schedules and workflows becomes harder as teams grow.
```

Then transition into:

`One platform. One workspace. One source of truth.`

The separate problem cards can subtly move toward a central platform to visualize consolidation.

## Workforce & HRM

Heading:

`Know your people. Empower your teams.`

Show a product preview using sanitized data:

```text
Employees

Employee       Department       Status
Employee 01    Engineering      Active
Employee 02    Operations       Active
Employee 03    Finance          Leave
```

Highlight:
- Employee Management
- Teams & Departments
- Attendance
- Leave
- Work Schedules
- Documents

## Projects & Operations

Heading:

`From planning to completion.`

Visual workflow:

```text
TODO
 ↓
IN PROGRESS
 ↓
REVIEW
 ↓
COMPLETED ✓
```

Connect:

```text
Projects
   ↓
Tasks
   ↓
Claims
   ↓
Time Tracking
   ↓
Reports
```

Use motion to demonstrate workflow, not decoration.

## Reports & Analytics

Heading:

`Turn operational activity into visibility.`

Show:
- Basic reports
- Advanced reports
- Analytics
- Workforce metrics

Use a product-style chart with fictional/demo values only.

## Security & Multi-Tenancy

Heading:

`Built for organizations that care about their data.`

Explain the security architecture in business-friendly language.

Architecture diagram:

```text
                    SaaS Platform
                          │
             ┌────────────┼────────────┐
             ↓            ↓            ↓
          Tenant A     Tenant B     Tenant C
             │            │            │
          Schema A     Schema B     Schema C
             │            │            │
         Isolated      Isolated      Isolated
           Data          Data          Data
```

Show verified capabilities already present in the project:
- Schema-per-tenant isolation
- JWT authentication
- Role-based authorization
- Refresh token rotation
- Audit logging
- Secure Stripe webhook verification
- Cross-tenant isolation testing

Do not claim compliance certifications that do not exist.

Security animation:

```text
SaaS Platform
      ↓
Tenant resolution
      ↓
Schema selection
      ↓
Tenant-isolated data
```

Keep it subtle and professional.

## Subscription / Pricing

Place pricing after product value and security have been established.

Heading:

`Plans that scale with your business.`

Subheading:

`Choose the capabilities your organization needs today, with room to grow as your team grows.`

Show three tiers:

```text
┌────────────────────┐
│ STARTER            │
│ For smaller teams  │
│                    │
│ ✓ Included feature │
│ ✓ Included feature │
│ ✓ Included feature │
│                    │
│ [ Get Started → ]  │
└────────────────────┘

┌────────────────────┐
│ PRO                │
│ For growing teams  │
│                    │
│ ✓ Everything       │
│ ✓ Advanced feature │
│ ✓ Advanced feature │
│                    │
│ [ Get Started → ]  │
└────────────────────┘

┌────────────────────┐
│ ENTERPRISE         │
│ For larger teams   │
│                    │
│ ✓ Everything       │
│ ✓ Custom workflows │
│ ✓ Advanced         │
│   integrations     │
│                    │
│ [ Get Started → ]  │
└────────────────────┘
```

Do NOT invent prices.

Inspect the actual backend/Stripe plan configuration and use the authoritative configured values.

## Subscription Feature Matrix

Use the actual entitlement configuration. Conceptual starting point:

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

This is a conceptual example. The repository's entitlement configuration is authoritative.

## Feature Entitlement Explanation

Add:

`Your subscription controls your platform capabilities.`

Visual:

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

Copy:

`Each subscription plan includes a defined set of platform capabilities. Your organization receives access according to its active subscription, and feature access is enforced by the platform backend.`

Do not create separate databases per plan.

The architecture remains:

```text
Tenant → Tenant Schema → Tenant Data

Tenant → Subscription → Plan → Feature Entitlements
```

## Pricing CTA

Plan buttons should enter the existing signup/onboarding flow.

Preferred if supported:

```text
Starter → /signup?plan=starter
Pro → /signup?plan=pro
Enterprise → /signup?plan=enterprise
```

Only use query parameters if the existing signup/backend flow safely supports them. Otherwise route all to `/signup` and let the existing subscription flow handle plan selection.

Do not create a second subscription system for the landing page.

## How It Works

Heading:

`Get your organization up and running.`

```text
01
Create your workspace
        ↓
02
Invite your team
        ↓
03
Configure roles & workflows
        ↓
04
Run your organization
```

Reveal each step as the user scrolls.

## Final CTA

```text
Bring your people,
projects and operations together.

One secure workspace for your organization.

              [ Get Started → ]
```

Get Started → `/signup`

Also show:
`Already have an account? Log in`

## Footer

Use only real routes:

```text
SaaS Platform
Enterprise Workforce & HRM

Product
Features
Security
Pricing

Company
Contact

Legal
Privacy
Terms

© 2026 SaaS Platform
```

## Motion System

Use a restrained motion language.

Section reveal:

```text
opacity: 0 → 1
translateY: 20px → 0
duration: 500–700ms
```

Cards:
- Stagger 60–100ms
- Same basic reveal style

Dashboard:
```text
opacity: 0 → 1
scale: 0.94 → 1
translateY: 40px → 0
duration: 800–1000ms
```

Buttons:
- 150–200ms hover
- Arrow moves only 3–4px
- Slight shadow/border emphasis

Navbar:
- 300–400ms transition on scroll

Parallax:
- Keep extremely subtle, approximately 15–25px movement
- Reduce/disable on mobile

Respect:

```css
@media (prefers-reduced-motion: reduce)
```

Remove large transforms/parallax/chart animations for reduced-motion users.

## Responsive Design

Desktop:
- Large hero
- Product previews
- Three pricing columns
- Rich feature layouts

Tablet:
- Two-column layouts where appropriate
- Reduced preview size

Mobile:
```text
Navbar
↓
Hero
↓
CTA
↓
Product Preview
↓
Features
↓
Security
↓
Pricing cards stacked
↓
How It Works
↓
Final CTA
↓
Footer
```

Do not reproduce desktop complexity on mobile.

## Suggested Components

```text
frontend/src/
├── pages/
│   └── LandingPage.tsx
└── components/
    └── landing/
        ├── LandingNavbar.tsx
        ├── HeroSection.tsx
        ├── ProductPreview.tsx
        ├── PlatformOverview.tsx
        ├── ProblemSolutionSection.tsx
        ├── WorkforceSection.tsx
        ├── OperationsSection.tsx
        ├── AnalyticsSection.tsx
        ├── SecuritySection.tsx
        ├── PricingSection.tsx
        ├── PricingComparison.tsx
        ├── HowItWorks.tsx
        ├── FinalCTA.tsx
        └── LandingFooter.tsx
```

Reuse existing components where possible.

## Data and Security Rules

- Never expose real tenant data.
- Never expose real user data.
- Never expose private subscription data.
- Never expose internal IDs.
- Demo product previews may use sanitized fictional data.
- Do not invent pricing.
- Do not invent customer claims.
- Do not expose Stripe secrets.

## Implementation Process

Before coding:

1. Inspect AppRoutes.tsx.
2. Inspect authentication guards.
3. Inspect existing theme.
4. Inspect shared UI components.
5. Inspect DashboardPage.tsx and major application pages.
6. Inspect existing assets/branding.
7. Inspect package.json.
8. Inspect subscription and feature entitlement implementation.
9. Identify reusable product UI for landing previews.
10. Report findings before major changes.

Then:

1. Create LandingPage.
2. Create reusable landing components.
3. Wire `/` to LandingPage.
4. Preserve `/login`, `/signup`, `/accept-invite`, `/app/*`.
5. Implement responsive design.
6. Implement restrained animations.
7. Implement pricing and entitlement presentation.
8. Wire CTAs.
9. Test public and authenticated routing.
10. Run lint and production build.
11. Fix all introduced errors/warnings.

## Performance

Keep the landing page fast.

Avoid:
- Huge images
- Video backgrounds
- Heavy 3D
- Duplicate animation libraries
- Unnecessary dependencies

Inspect package.json first. Reuse existing animation/UI libraries if already installed.

## SEO

Use appropriate public metadata.

Suggested title:

`SaaS Platform — Enterprise Workforce & Business Operations`

Suggested description:

`Manage your workforce, projects, time, schedules and business operations from one secure multi-tenant SaaS platform.`

Use the existing metadata approach.

## Acceptance Criteria

The implementation is complete when:

- `/` displays a public landing page.
- Unauthenticated visitors are not immediately redirected to login.
- Login works.
- Get Started works.
- Existing `/app` routes remain functional.
- Hero clearly explains the product.
- Product UI is shown visually.
- Major capabilities are explained.
- Security/multi-tenancy is explained.
- Subscription tiers are displayed.
- Benefits/features are associated with tiers.
- Pricing is not fabricated.
- Feature terminology matches the entitlement system.
- Subscription → Plan → Entitlements → Authorization is explained.
- CTAs work.
- Mobile layout works.
- Theme support works if already supported.
- Reduced-motion preferences are respected.
- No fake customer claims are introduced.
- No real tenant/user data is exposed.
- `npm run lint` passes.
- `npm run build` passes.

## Final User Journey

```text
                    PUBLIC VISITOR
                         │
                         ▼
                 ┌───────────────┐
                 │    LANDING    │
                 └───────┬───────┘
                         │
          ┌──────────────┼──────────────┐
          ↓              ↓              ↓
       Product        Security        Pricing
          │              │              │
          └──────────────┼──────────────┘
                         ↓
                   Get Started
                         │
                         ▼
                      Signup
                         │
                         ▼
                  Create Workspace
                         │
                         ▼
                  Select Subscription
                         │
                         ▼
                       Stripe
                         │
                         ▼
                  Tenant Subscription
                         │
                         ▼
                  Feature Entitlements
                         │
                         ▼
                    SaaS Platform
```
