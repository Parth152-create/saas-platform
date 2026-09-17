# FRONTEND IMPLEMENTATION & UI CORRECTIONS

You are responsible for implementing and completing the frontend of this SaaS platform.

Before writing code, read:

1. ../PROJECT_SPEC.md
2. ./FRONTEND_SPEC.md

The repository is the source of truth. Inspect the existing frontend and backend before making implementation decisions.

==================================================
1. PRIMARY OBJECTIVE
==================================================

Build the frontend into a production-quality SaaS application based on:

- FRONTEND_SPEC.md
- PROJECT_SPEC.md
- The existing backend implementation
- The provided UI/design direction

Do not blindly follow assumptions in the specification if they conflict with the actual repository.

Inspect the backend controllers, DTOs, security configuration, authentication flow, Google OIDC configuration, billing APIs, CORS configuration, and available endpoints before integrating functionality.

Do not modify working backend functionality unless a genuine frontend-blocking issue is discovered.

==================================================
2. GLOBAL VISUAL DESIGN
==================================================

IMPORTANT:

REMOVE THE CURRENT BLUE/PURPLE VISUAL IDENTITY.

The entire application must use a premium monochrome BLACK + WHITE + GRAYSCALE design system.

Do NOT use:

- Purple backgrounds
- Blue backgrounds
- Purple gradients
- Blue gradients
- Purple glowing effects
- Blue glowing effects
- Colorful glassmorphism
- Neon backgrounds
- Large decorative color gradients

The visual language should be:

- Minimal
- Premium
- Professional
- Enterprise SaaS
- Clean
- Modern
- High contrast
- Restrained
- Consistent

Semantic colors are allowed ONLY where they communicate meaning.

For example:

- Green → success
- Red → error/destructive
- Yellow/amber → warning
- Other colors → only when genuinely required by data visualization

These colors must not become part of the overall branding.

==================================================
3. LIGHT MODE
==================================================

Light mode should use:

- White / near-white page background
- White cards
- Black / near-black primary text
- Gray secondary text
- Light gray borders
- Black primary buttons
- White text on primary buttons
- Subtle shadows only where useful

The UI should feel clean rather than colorful.

==================================================
4. DARK MODE
==================================================

Dark mode should use:

- Near-black page background
- Dark charcoal cards
- Dark gray borders
- White primary text
- Muted gray secondary text
- White primary buttons
- Black text on primary buttons

Do NOT turn dark mode into a purple/blue theme.

It should be genuinely monochrome.

==================================================
5. THEME TOGGLE
==================================================

The current theme toggle does not work correctly.

Fix it completely.

Requirements:

- Light mode works globally.
- Dark mode works globally.
- Theme changes immediately.
- Theme persists after page refresh.
- System theme can be respected when no explicit preference exists.
- Login page supports both themes.
- Signup page supports both themes.
- Dashboard supports both themes.
- Sidebar supports both themes.
- Header supports both themes.
- Tables support both themes.
- Forms support both themes.
- Modals/dropdowns support both themes.
- Charts remain readable in both themes.

Use one centralized theme mechanism.

Do NOT create separate theme state inside individual components.

Use centralized CSS variables/design tokens.

At minimum define semantic tokens such as:

--background
--foreground
--card
--card-foreground
--primary
--primary-foreground
--secondary
--muted
--muted-foreground
--border
--input
--sidebar
--sidebar-foreground

Avoid hardcoded colors throughout components.

==================================================
6. SIDEBAR
==================================================

The existing sidebar expand/collapse interaction is working.

Keep it.

However, the logo is currently being cut in half when the sidebar becomes compact.

Fix this.

EXPANDED SIDEBAR:

- Show the complete logo.
- Logo must not be clipped.
- Product/company name can appear beside the logo.
- Maintain proper spacing.

COMPACT SIDEBAR:

- Never crop the logo.
- Never squeeze the logo.
- Never display half of the logo.
- If a compact logo/mark exists, use it.
- Otherwise scale the complete logo proportionally.
- Center the logo.
- Keep proper padding.

Also ensure:

- Navigation icons remain centered.
- Labels disappear cleanly.
- Active state remains visible.
- Tooltips work in compact mode.
- Collapse button remains accessible.
- Sidebar animation is smooth.
- No horizontal overflow.

==================================================
7. LOGIN PAGE
==================================================

The login page must use the same monochrome visual system.

Do not copy the purple/blue visual treatment from the reference screenshot.

Structure:

                 [ LOGO ]

              Login to Dashboard

        ┌─────────────────────────────┐
        │  G  Continue with Google    │
        └─────────────────────────────┘

                    ── or ──

        Email
        ┌─────────────────────────────┐
        │ you@example.com             │
        └─────────────────────────────┘

        Password
        ┌─────────────────────────────┐
        │ •••••••••••••••          ◉  │
        └─────────────────────────────┘

        □ Remember me      Forgot password?

        ┌─────────────────────────────┐
        │            Login             │
        └─────────────────────────────┘

           Don't have an account?
                Create account

Use:

- Clean typography
- Clear labels
- Thin borders
- Black primary action in light mode
- White primary action in dark mode
- Proper focus states
- Password visibility toggle
- Validation states
- Loading state
- Error state

==================================================
8. GOOGLE LOGIN — MUST BE FUNCTIONAL
==================================================

The current "Sign in with Google" button does not work.

Fix the actual authentication flow.

DO NOT create a fake Google login.

Inspect the existing Spring Boot backend and identify:

- Google OIDC configuration
- Authorization endpoint
- Callback/redirect flow
- Authentication success behavior
- Frontend redirect expectations
- Required environment configuration

Then connect the frontend Google button to the REAL backend flow.

If the backend expects browser redirection, perform the appropriate browser redirect.

Do not expose:

- Google client secrets
- JWT private keys
- Backend secrets

in frontend code.

Do NOT add Apple login.

The backend currently supports Google authentication, so the frontend should implement Google authentication based on the actual backend contract.

==================================================
9. SIGNUP PAGE
==================================================

Create a clean signup page using the exact same monochrome authentication design language.

Do NOT use:

- Purple
- Blue
- Gradients
- Decorative colorful backgrounds
- Unnecessary illustrations

The signup page should be simple and professional.

Desktop structure:

                 [ LOGO ]

           Create your account

        Set up your account to get started.

        Full name
        ┌─────────────────────────────┐
        │ Enter your full name        │
        └─────────────────────────────┘

        Email
        ┌─────────────────────────────┐
        │ you@example.com             │
        └─────────────────────────────┘

        Password
        ┌─────────────────────────────┐
        │ Create a password        ◉  │
        └─────────────────────────────┘

        Confirm password
        ┌─────────────────────────────┐
        │ Confirm your password    ◉  │
        └─────────────────────────────┘

        ┌─────────────────────────────┐
        │       Create account        │
        └─────────────────────────────┘

        Already have an account?
                 Log in

SIGNUP FIELDS:

1. Full name
2. Email
3. Password
4. Confirm password

IMPORTANT:

Before implementing the form, inspect the actual backend signup DTO/controller.

If the backend requires additional fields, use the actual backend contract.

Do NOT invent fields.

==================================================
10. SIGNUP FORM BEHAVIOR
==================================================

Implement:

- Client-side validation
- Required-field validation
- Valid email validation
- Password requirements based on backend rules
- Confirm-password validation
- Password visibility toggle
- Loading state
- Disabled submit while submitting
- Backend validation error handling
- Network error handling
- Server error handling
- Successful signup handling

Button states:

Normal:
"Create account"

Submitting:
"Creating account..."

Do not allow duplicate submissions.

If password and confirm password differ:

"Passwords do not match."

Display validation errors close to the relevant field.

==================================================
11. SIGNUP + GOOGLE
==================================================

Do NOT automatically add Google signup just because Google login exists.

First inspect the backend's intended Google OIDC flow.

If the backend supports Google authentication as an account-creation path, the signup page may provide:

"Continue with Google"

using the exact same real OAuth flow.

If the backend is designed for Google authentication only through the login flow, keep Google authentication on the login page and do not invent a separate signup endpoint.

The backend contract is authoritative.

==================================================
12. LOGIN/SIGNUP CONSISTENCY
==================================================

Login and signup must feel like the same product.

Use the same:

- Logo
- Typography
- Input components
- Button components
- Border styles
- Spacing system
- Theme system
- Error handling
- Loading states
- Responsive behavior

The user should immediately understand that both pages belong to the same application.

==================================================
13. AUTHENTICATION ARCHITECTURE
==================================================

Inspect the actual backend authentication implementation.

Implement:

- Login
- Signup
- Google OIDC
- Access token handling
- Refresh token handling
- Logout
- Protected routes
- Authentication state
- Session expiration handling
- Unauthorized handling

Use the actual backend response structure.

Do not guess field names.

The backend's actual token response and endpoint structure must be verified before implementation.

==================================================
14. API CLIENT
==================================================

Create a centralized API client.

Do not scatter raw API requests throughout components.

The API layer should handle:

- Base URL
- Authentication headers
- JSON handling
- API errors
- Token refresh
- Unauthorized responses
- Consistent error handling

Use environment variables for configurable API URLs.

Verify the actual backend port/configuration before hardcoding anything.

==================================================
15. MAIN APPLICATION
==================================================

Implement the application shell and navigation described in FRONTEND_SPEC.md.

Navigation includes:

- Dashboard
- Projects
- Claims
- Tasks
- Schedule
- Time Tracking
- Reports
- HRM

HRM includes:

- Employee Management
- Teams & Departments
- Attendance & Leave
- Work Schedule Models
- Time Tracking
- Documents

Settings includes:

- Company Settings
- Work Schedule Models
- Absence Types
- User Management
- Roles & Permissions
- Integrations
- System Settings

==================================================
16. DASHBOARD
==================================================

Implement the dashboard according to FRONTEND_SPEC.md.

Include the required:

- KPI cards
- Charts
- Summary widgets
- Activity sections
- Tables
- Widget Library
- Loading states
- Empty states
- Error states

Follow the monochrome visual identity.

Do not create a generic colorful admin dashboard.

==================================================
17. HRM
==================================================

Implement the HRM UI described in FRONTEND_SPEC.md.

Include:

- HRM overview
- Employee list
- Employee search
- Employee filtering
- Employee profile
- Employee details
- Employee onboarding
- Employee invitation
- Teams
- Departments
- Attendance
- Leave
- Work schedules
- Time tracking
- Documents

Use real APIs wherever available.

==================================================
18. BILLING
==================================================

Implement the billing UI using the actual backend APIs.

Include:

- Current subscription
- Subscription status
- Billing summary
- Invoice information
- Checkout
- Stripe Customer Portal
- Billing loading states
- Billing error states

Do not expose Stripe secrets.

Use the actual backend billing endpoints and DTOs.

==================================================
19. RBAC
==================================================

The backend RBAC hierarchy is:

SUPER_ADMIN
ADMIN
MANAGER
USER

The frontend should use the authenticated user's role to control UI visibility and available actions.

However:

Frontend RBAC is NOT the security boundary.

The backend remains authoritative.

Do not assume hiding a button provides security.

==================================================
20. RESPONSIVE DESIGN
==================================================

Everything must work on:

- Desktop
- Laptop
- Tablet
- Mobile

Especially verify:

- Sidebar
- Header
- Tables
- Forms
- Login
- Signup
- Dashboard cards
- Charts
- Modals
- Navigation

No horizontal scrolling caused by broken layouts.

==================================================
21. ACCESSIBILITY
==================================================

Implement:

- Keyboard navigation
- Visible focus states
- Proper labels
- Accessible buttons
- Accessible inputs
- Appropriate ARIA attributes where required
- Sufficient contrast
- Tooltips for compact sidebar icons
- Accessible modal behavior

==================================================
22. DATA RULE
==================================================

Never make fake data appear to be real tenant data.

If an API exists:

USE THE API.

If an API does not exist:

Build the UI so it is ready for the future API.

If temporary mock data is absolutely necessary:

Keep it isolated under an obvious mock/demo location and make it easy to replace.

==================================================
23. CODE QUALITY
==================================================

Use:

- React
- TypeScript
- Reusable components
- Strong typing
- Clean folder structure
- Centralized API logic
- Centralized theme system
- Maintainable state management
- Small focused components

Avoid:

- Giant components
- Duplicated API calls
- Duplicated UI logic
- Hardcoded API URLs
- Hardcoded colors everywhere
- Unnecessary dependencies
- Dead code
- Console errors
- Temporary hacks left in production code

==================================================
24. IMPLEMENTATION WORKFLOW
==================================================

Follow this order:

1. Inspect repository.
2. Inspect existing frontend.
3. Inspect backend APIs.
4. Inspect authentication implementation.
5. Inspect Google OIDC implementation.
6. Inspect current theme implementation.
7. Inspect sidebar/logo implementation.
8. Fix global theme.
9. Fix sidebar compact mode.
10. Implement/fix login.
11. Implement/fix Google authentication.
12. Implement signup.
13. Implement API client/auth state.
14. Implement application shell.
15. Implement dashboard.
16. Implement HRM.
17. Implement billing.
18. Implement settings.
19. Implement remaining modules.
20. Add responsive behavior.
21. Add accessibility.
22. Polish the UI.
23. Run build/tests.
24. Fix all errors.

==================================================
25. VERIFICATION
==================================================

Before declaring the frontend complete, verify:

THEME:
- Light mode works.
- Dark mode works.
- Theme persists after refresh.
- No purple/blue visual identity remains.

SIDEBAR:
- Expanded mode works.
- Compact mode works.
- Logo is never clipped.
- Icons remain centered.
- Tooltips work.

AUTH:
- Login works.
- Signup works.
- Google login works using the actual backend OAuth flow.
- Password visibility works.
- Validation works.
- Loading states work.
- Error states work.
- Protected routes work.
- Logout works.
- Refresh-token behavior works according to backend implementation.

UI:
- Dashboard works.
- HRM works.
- Billing works.
- Settings work.
- Navigation works.
- Responsive layouts work.

QUALITY:
- No TypeScript errors.
- No build errors.
- No runtime errors.
- No unnecessary console errors.
- No broken routes.
- No fake data presented as real functionality.

Run the production build and fix every issue you encounter.

==================================================
26. FINAL RULE
==================================================

Do not stop at a visual mockup.

The objective is a FUNCTIONAL, PRODUCTION-QUALITY FRONTEND connected to the existing Spring Boot backend.

When a backend API already exists, integrate with it.

When something is unclear, inspect the backend rather than guessing.

Do not rewrite working backend functionality.

Start by inspecting the repository and both specification files, then implement the frontend systematically.