// Type definitions exactly matching Spring Boot DTOs and entities

export type Role = 'SUPER_ADMIN' | 'ADMIN' | 'MANAGER' | 'USER';

export interface UserProfile {
  id: string;
  email: string;
  role: Role;
  tenantId: string;
}

export interface SignupRequest {
  tenantId: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  tenantId: string;
  email: string;
  password: string;
}

export interface GoogleLoginRequest {
  idToken: string;
  tenantId: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface AcceptInviteRequest {
  tenantId: string;
  token: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface InviteUserRequest {
  email: string;
  role: Role;
}

export interface InviteUserResponse {
  email: string;
  role: Role;
  inviteToken: string;
  expiresAt: string;
}

export type PlanTier = 'FREE' | 'PRO' | 'ENTERPRISE';

export type SubscriptionStatus =
  | 'ACTIVE'
  | 'TRIALING'
  | 'CANCELED'
  | 'INCOMPLETE'
  | 'PAST_DUE'
  | 'UNPAID';

export type InvoiceStatus = 'DRAFT' | 'OPEN' | 'PAID' | 'UNCOLLECTIBLE' | 'VOID';

export interface SubscriptionResponse {
  status: SubscriptionStatus;
  stripeSubscriptionId: string;
  stripePriceId: string;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  cancelAtPeriodEnd: boolean;
}

export interface InvoiceResponse {
  stripeInvoiceId: string;
  status: InvoiceStatus;
  amountDueCents: number;
  amountPaidCents: number;
  currency: string;
  hostedInvoiceUrl?: string;
  invoicePdfUrl?: string;
  paidAt?: string;
}

export interface BillingSummaryResponse {
  plan: string;
  subscription: SubscriptionResponse | null;
  invoices: InvoiceResponse[];
}

export interface CreateCheckoutSessionRequest {
  planTier: PlanTier;
}

export interface CreateCheckoutSessionResponse {
  checkoutUrl: string;
}

export interface CreatePortalSessionResponse {
  url: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: string[];
}
