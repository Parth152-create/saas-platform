import type { ApiErrorResponse, TokenResponse } from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090';

const ACCESS_TOKEN_KEY = 'saas_access_token';
const REFRESH_TOKEN_KEY = 'saas_refresh_token';
const USER_INFO_KEY = 'saas_user_info';

export interface StoredUser {
  id: string;
  email: string;
  role: string;
  tenantId: string;
}

export function getStoredAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getStoredRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function getStoredUser(): StoredUser | null {
  const data = localStorage.getItem(USER_INFO_KEY);
  if (!data) return null;
  try {
    return JSON.parse(data) as StoredUser;
  } catch {
    return null;
  }
}

export function saveAuthSession(tokens: TokenResponse, user: StoredUser): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(user));
}

export function clearAuthSession(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_INFO_KEY);
}

// Queue mechanism for handling in-flight refresh requests
let isRefreshing = false;
let refreshSubscribers: ((token: string | null) => void)[] = [];

function subscribeTokenRefresh(cb: (token: string | null) => void) {
  refreshSubscribers.push(cb);
}

function onRefreshed(token: string | null) {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
}

let onUnauthorizedCallback: (() => void) | null = null;

export function setUnauthorizedCallback(callback: () => void): void {
  onUnauthorizedCallback = callback;
}

export class ApiError extends Error {
  status: number;
  error: string;
  details?: string[];
  path?: string;

  constructor(status: number, error: string, message: string, details?: string[], path?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.error = error;
    this.details = details;
    this.path = path;
  }
}

async function tryRefreshToken(): Promise<string | null> {
  const currentRefreshToken = getStoredRefreshToken();
  if (!currentRefreshToken) {
    return null;
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken: currentRefreshToken }),
    });

    if (!response.ok) {
      clearAuthSession();
      if (onUnauthorizedCallback) {
        onUnauthorizedCallback();
      }
      return null;
    }

    const data = (await response.json()) as TokenResponse;
    const existingUser = getStoredUser();
    if (existingUser) {
      saveAuthSession(data, existingUser);
    } else {
      localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
    }
    return data.accessToken;
  } catch {
    clearAuthSession();
    if (onUnauthorizedCallback) {
      onUnauthorizedCallback();
    }
    return null;
  }
}

export async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const token = getStoredAccessToken();
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(url, { ...options, headers });
  } catch (err) {
    throw new ApiError(0, 'Network Error', err instanceof Error ? err.message : 'Network request failed');
  }

  // Handle 401 Unauthorized with token refresh rotation
  if (response.status === 401 && !endpoint.includes('/api/auth/login') && !endpoint.includes('/api/auth/refresh')) {
    if (!isRefreshing) {
      isRefreshing = true;
      const newToken = await tryRefreshToken();
      isRefreshing = false;
      onRefreshed(newToken);

      if (newToken) {
        const retryHeaders = new Headers(options.headers || {});
        if (!retryHeaders.has('Content-Type') && !(options.body instanceof FormData)) {
          retryHeaders.set('Content-Type', 'application/json');
        }
        retryHeaders.set('Authorization', `Bearer ${newToken}`);
        return request<T>(endpoint, { ...options, headers: retryHeaders });
      }
    } else {
      // Wait for existing refresh to complete
      return new Promise<T>((resolve, reject) => {
        subscribeTokenRefresh(async (newToken) => {
          if (!newToken) {
            reject(new ApiError(401, 'Unauthorized', 'Session expired. Please log in again.'));
            return;
          }
          const retryHeaders = new Headers(options.headers || {});
          if (!retryHeaders.has('Content-Type') && !(options.body instanceof FormData)) {
            retryHeaders.set('Content-Type', 'application/json');
          }
          retryHeaders.set('Authorization', `Bearer ${newToken}`);
          try {
            const result = await request<T>(endpoint, { ...options, headers: retryHeaders });
            resolve(result);
          } catch (retryErr) {
            reject(retryErr);
          }
        });
      });
    }
  }

  if (!response.ok) {
    let errorData: Partial<ApiErrorResponse> = {};
    try {
      errorData = await response.json();
    } catch {
      // Response body wasn't JSON
    }

    const message =
      errorData.message ||
      (errorData.details && errorData.details.length > 0 ? errorData.details.join(', ') : response.statusText);

    throw new ApiError(
      response.status,
      errorData.error || response.statusText,
      message || `HTTP ${response.status} ${response.statusText}`,
      errorData.details,
      errorData.path
    );
  }

  // 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  return (await response.json()) as T;
}

export const apiClient = {
  get: <T>(endpoint: string, options?: RequestInit) =>
    request<T>(endpoint, { ...options, method: 'GET' }),

  post: <T>(endpoint: string, body?: unknown, options?: RequestInit) =>
    request<T>(endpoint, {
      ...options,
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    }),

  put: <T>(endpoint: string, body?: unknown, options?: RequestInit) =>
    request<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    }),

  delete: <T>(endpoint: string, options?: RequestInit) =>
    request<T>(endpoint, { ...options, method: 'DELETE' }),
};
