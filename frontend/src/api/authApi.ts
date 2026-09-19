import { apiClient } from './apiClient';
import type {
  AcceptInviteRequest,
  GoogleLoginRequest,
  LoginRequest,
  RefreshRequest,
  SignupRequest,
  TokenResponse,
} from './types';

export const authApi = {
  signup: (req: SignupRequest): Promise<TokenResponse> =>
    apiClient.post<TokenResponse>('/api/auth/signup', req),

  login: (req: LoginRequest): Promise<TokenResponse> =>
    apiClient.post<TokenResponse>('/api/auth/login', req),

  googleLogin: (req: GoogleLoginRequest): Promise<TokenResponse> =>
    apiClient.post<TokenResponse>('/api/auth/google', req),

  refreshToken: (refreshToken: string): Promise<TokenResponse> =>
    apiClient.post<TokenResponse>('/api/auth/refresh', { refreshToken } as RefreshRequest),

  acceptInvite: (req: AcceptInviteRequest): Promise<TokenResponse> =>
    apiClient.post<TokenResponse>('/api/auth/accept-invite', req),

  logout: (refreshToken: string): Promise<void> =>
    apiClient.post<void>('/api/auth/logout', { refreshToken }),
};
