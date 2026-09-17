import type { Role } from '../api/types';

export interface DecodedToken {
  sub: string;
  tenant_id?: string;
  role?: Role;
  exp?: number;
  iss?: string;
}

export function parseJwt(token: string): DecodedToken | null {
  try {
    const base64Url = token.split('.')[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload) as DecodedToken;
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string, bufferSeconds = 30): boolean {
  const decoded = parseJwt(token);
  if (!decoded || !decoded.exp) return true;
  const nowInSeconds = Math.floor(Date.now() / 1000);
  return decoded.exp - bufferSeconds < nowInSeconds;
}
