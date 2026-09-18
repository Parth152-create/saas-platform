# Nexa — Brand Identity & Rebranding Specification

## Brand

The product brand is:

**Nexa**

Descriptor:

**WORKFORCE & OPERATIONS**

Do NOT use `NexaOS` or `Nexa OS`.

The repository name, local folder, Java packages, API paths, database names, and other technical identifiers do not need to be renamed. This is a user-facing product rebrand.

## Positioning

Nexa is an enterprise workforce and business operations platform.

Primary line:

> Your organization, connected in one workspace.

Supporting line:

> Manage people, projects, time, workflows and operations from one secure workspace.

The brand should communicate workforce management, connected workflows, security, scalability, and professional enterprise software.

## Logo

Create a custom geometric abstract `N` mark.

The mark should communicate:
- Connection
- People
- Workflow
- Movement
- Organization
- Technology

Do NOT use a typed N as the logo.

Do NOT use a generic cloud, shield, globe, infinity symbol, lightning bolt, or generic hexagon.

The mark must work at:
- 16x16 favicon
- 32x32 favicon
- 64x64 icon
- Sidebar
- Navbar
- Landing page
- Login/signup
- Footer

Create:
1. Primary dark logo
2. Primary light logo
3. Mark-only icon
4. Favicon
5. Wordmark

Primary lockup:

```text
[N geometric mark]  Nexa
                     WORKFORCE & OPERATIONS
```

Compact:

```text
[N] Nexa
```

Collapsed:

```text
[N]
```

Favicon:

```text
[N]
```

## Colors

Use the existing enterprise SaaS visual system:

```text
Background Dark:       #0A0A0A
Surface Dark:          #141414
Surface Elevated:      #1F1F23
Border Dark:           #262626

Primary Blue:          #2563EB

Text Primary Dark:     #FFFFFF
Text Secondary Dark:   #A3A3A3

Background Light:      #FFFFFF
Text Primary Light:    #09090B
Text Secondary Light:  #525252
```

Use blue selectively for CTAs, highlights, active states, and brand accents.

Avoid excessive gradients.

## Typography

Prefer the existing application typography. If a brand font needs to be selected, use a clean modern sans-serif such as Inter.

Wordmark:
- Modern
- Geometric
- Medium/bold
- Tight but readable kerning

Descriptor:
- Uppercase
- Small
- Increased letter spacing
- Medium weight

## Landing Page Branding

Hero eyebrow:

`WORKFORCE & OPERATIONS`

Preferred headline:

`Your organization, connected in one workspace.`

Supporting copy:

`Manage people, projects, time, workflows and operations from one secure workspace.`

CTAs:

`[ Get Started → ]    [ Log In ]`

Get Started → `/signup`

Login → `/login`

The arrow should move approximately 3–4px right on hover.

## Existing UI Rebrand

Search the entire frontend for:

- SaaS Platform
- SaaS PLATFORM
- Enterprise HRM
- NexaOS
- Nexa OS

Replace user-facing product branding with:

```text
Nexa
WORKFORCE & OPERATIONS
```

Do not blindly replace technical/internal identifiers.

Update where applicable:
- Landing navbar
- Landing footer
- Landing hero
- Login
- Signup
- Accept Invite
- AppShell
- Sidebar
- Dashboard header
- Loading states
- Error pages
- Browser title
- Favicon
- PWA metadata
- SEO metadata
- Public metadata

## Sidebar

Use:

```text
[N mark] Nexa
          WORKFORCE & OPERATIONS
```

Collapsed:

```text
[N]
```

Keep existing navigation unchanged.

## Login / Signup

Login:

```text
[N MARK]

Nexa
WORKFORCE & OPERATIONS

Welcome back
Sign in to your workspace
```

Signup:

```text
[N MARK]

Nexa
WORKFORCE & OPERATIONS

Create your workspace
```

Do not alter authentication behavior.

## Favicon

Replace the current generic favicon with the Nexa geometric mark.

Requirements:
- High contrast
- No tiny text
- No descriptor
- No wordmark
- Recognizable at 16x16

Dark:
- Dark background
- White/blue mark

Light:
- White background
- Black/blue mark

## Browser Metadata

Title:

`Nexa — Workforce & Operations`

Description:

`Manage your workforce, projects, time, workflows and business operations from one secure workspace.`

Use the existing metadata implementation.

## Logo Asset Strategy

Prefer SVG.

Suggested assets:

```text
frontend/public/
├── nexa-mark.svg
├── nexa-logo-dark.svg
├── nexa-logo-light.svg
├── favicon.svg
└── ...
```

Follow existing asset conventions if different.

SVGs must be clean, scalable, vector-based, and free of embedded raster images.

## Logo Generation Prompt

```text
Design a premium enterprise SaaS logo for a product called "Nexa".

Brand descriptor:
"WORKFORCE & OPERATIONS"

Create a distinctive geometric symbol based on an abstract letter N.
The N should be formed from clean diagonal and vertical segments with
intentional negative space.

The symbol should communicate connection, people, workflow, movement,
organization and modern enterprise technology.

Style:
- Minimal
- Geometric
- Premium
- Professional
- Enterprise SaaS
- Modern
- Timeless
- Highly recognizable

Colors:
- Black #0A0A0A
- White #FFFFFF
- Electric blue #2563EB
- Optional extremely subtle blue gradient only if necessary

Create:
1. Primary dark logo
2. Primary light logo
3. Mark-only icon
4. Favicon version
5. Wordmark

The wordmark must read exactly:
Nexa

The descriptor must read exactly:
WORKFORCE & OPERATIONS

Do NOT write "NexaOS".
Do NOT write "Nexa OS".
Do NOT use "OS" anywhere.
Do NOT use a lightning bolt.
Do NOT use a generic cloud, shield, globe, infinity symbol, or generic hexagon.
Do NOT use excessive gradients.
Do NOT use 3D effects.
Do NOT use mockups as the final logo asset.

The logo must remain recognizable at 16x16 favicon size.
```

## Coding Agent Prompt

```text
Read PROJECT_SPEC.md first.

Then read NEXA_BRANDING_SPEC.md.

The repository is the source of truth.

We are performing the final user-facing product branding pass.

The product brand is:

Nexa

Descriptor:

WORKFORCE & OPERATIONS

IMPORTANT:
The brand is NOT NexaOS and NOT Nexa OS.

First inspect the repository for:
- SaaS Platform
- SaaS PLATFORM
- Enterprise HRM
- NexaOS
- Nexa OS
- existing favicon/logo assets
- browser titles
- metadata
- sidebar branding
- landing branding
- auth-page branding

Report all relevant locations before making major changes.

Then implement the Nexa branding consistently across the frontend.

Requirements:

1. Replace user-facing "SaaS Platform" branding with "Nexa".
2. Use "WORKFORCE & OPERATIONS" as the descriptor.
3. Remove every user-facing reference to "NexaOS" and "Nexa OS".
4. Keep technical/internal identifiers such as repository names, Java packages,
   database names and API paths unchanged unless technically necessary.
5. Create/reuse SVG logo assets following the geometric Nexa N direction.
6. Replace the current generic S/logo treatment.
7. Replace the favicon with the Nexa mark.
8. Update browser title and public metadata.
9. Update LandingPage branding.
10. Update LandingNavbar and LandingFooter.
11. Update AppShell/Sidebar branding.
12. Update LoginPage branding.
13. Update SignupPage branding.
14. Update AcceptInvitePage branding if applicable.
15. Update loading/error/public states where branding is shown.
16. Preserve existing theme support.
17. Preserve existing routing.
18. Preserve authentication.
19. Preserve subscription and feature entitlement functionality.
20. Do not alter backend architecture for a branding-only change.

Use the existing design system.

Brand style:
- Dark enterprise SaaS
- Black/white neutral foundation
- Blue #2563EB as restrained accent
- Clean geometric logo
- Professional typography
- Minimal visual noise

Do not introduce excessive gradients, glassmorphism, neon effects,
3D elements, or unrelated UI redesigns.

After implementation:

1. Search the frontend again for "SaaS Platform".
2. Search for "NexaOS".
3. Search for "Nexa OS".
4. Verify only appropriate technical/internal references remain.
5. Run npm run lint.
6. Run npm run build.
7. Verify / landing page.
8. Verify /login.
9. Verify /signup.
10. Verify /app/dashboard.
11. Verify Settings → Subscription.
12. Verify dark/light themes.
13. Verify favicon/browser title.

Report exactly what changed and the verification results.
```

## Final Brand System

```text
                         NEXA

               WORKFORCE & OPERATIONS

       Your organization, connected in one workspace.
```

Product modules remain:

```text
Nexa
 │
 ├── Workforce & HRM
 ├── Projects
 ├── Tasks
 ├── Claims
 ├── Time Tracking
 ├── Reports & Analytics
 ├── Multi-Tenancy
 ├── RBAC
 ├── Billing
 └── Subscription Entitlements
```

Only the public product identity changes. The underlying architecture remains intact.
