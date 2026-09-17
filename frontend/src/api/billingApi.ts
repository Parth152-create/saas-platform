import { apiClient } from './apiClient';
import type {
  BillingSummaryResponse,
  CreateCheckoutSessionRequest,
  CreateCheckoutSessionResponse,
  CreatePortalSessionResponse,
  FeatureEntitlementsResponse,
} from './types';

export const billingApi = {
  getBillingSummary: (): Promise<BillingSummaryResponse> =>
    apiClient.get<BillingSummaryResponse>('/api/billing'),

  getEntitlements: (): Promise<FeatureEntitlementsResponse> =>
    apiClient.get<FeatureEntitlementsResponse>('/api/billing/entitlements'),

  createCheckoutSession: (
    req: CreateCheckoutSessionRequest
  ): Promise<CreateCheckoutSessionResponse> =>
    apiClient.post<CreateCheckoutSessionResponse>('/api/billing/checkout-session', req),

  createPortalSession: (): Promise<CreatePortalSessionResponse> =>
    apiClient.post<CreatePortalSessionResponse>('/api/billing/portal-session', {}),
};
