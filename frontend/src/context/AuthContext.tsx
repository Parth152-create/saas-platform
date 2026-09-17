import React, { createContext, useContext, useEffect, useState } from 'react';
import {
  clearAuthSession,
  getStoredAccessToken,
  getStoredRefreshToken,
  getStoredUser,
  saveAuthSession,
  setUnauthorizedCallback,
  type StoredUser,
} from '../api/apiClient';
import { authApi } from '../api/authApi';
import type {
  AcceptInviteRequest,
  LoginRequest,
  Role,
  SignupRequest,
  TokenResponse,
  UserProfile,
} from '../api/types';
import { parseJwt } from '../utils/jwt';

interface AuthContextType {
  user: UserProfile | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (req: LoginRequest) => Promise<void>;
  signup: (req: SignupRequest) => Promise<void>;
  googleLogin: (idToken: string, tenantId: string) => Promise<void>;
  acceptInvite: (req: AcceptInviteRequest) => Promise<void>;
  logout: () => void;
  hasRole: (requiredRole: Role) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const ROLE_HIERARCHY: Record<Role, number> = {
  SUPER_ADMIN: 4,
  ADMIN: 3,
  MANAGER: 2,
  USER: 1,
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const handleTokenSuccess = (
    tokens: TokenResponse,
    fallbackEmail: string,
    fallbackTenantId: string
  ): void => {
    const decoded = parseJwt(tokens.accessToken);
    const userId = decoded?.sub || 'unknown-user';
    const role = (decoded?.role || 'USER') as Role;
    const tenantId = decoded?.tenant_id || fallbackTenantId;

    const storedUser: StoredUser = {
      id: userId,
      email: fallbackEmail,
      role,
      tenantId,
    };

    saveAuthSession(tokens, storedUser);
    setAccessToken(tokens.accessToken);
    setUser({
      id: userId,
      email: fallbackEmail,
      role,
      tenantId,
    });
  };

  // Rehydrate session from storage on load
  useEffect(() => {
    const initAuth = async () => {
      const token = getStoredAccessToken();
      const stored = getStoredUser();
      const refreshToken = getStoredRefreshToken();

      if (token && stored) {
        const decoded = parseJwt(token);
        if (decoded && decoded.exp && decoded.exp * 1000 > Date.now()) {
          setAccessToken(token);
          setUser({
            id: stored.id,
            email: stored.email,
            role: (decoded.role || stored.role || 'USER') as Role,
            tenantId: decoded.tenant_id || stored.tenantId,
          });
        } else if (refreshToken) {
          // Token expired, attempt refresh
          try {
            const refreshed = await authApi.refreshToken(refreshToken);
            handleTokenSuccess(refreshed, stored.email, stored.tenantId);
          } catch {
            clearAuthSession();
            setUser(null);
            setAccessToken(null);
          }
        } else {
          clearAuthSession();
          setUser(null);
          setAccessToken(null);
        }
      }
      setIsLoading(false);
    };

    initAuth();

    // Register callback for when apiClient encounters unrecoverable 401
    setUnauthorizedCallback(() => {
      setUser(null);
      setAccessToken(null);
    });
  }, []);

  const login = async (req: LoginRequest): Promise<void> => {
    const tokens = await authApi.login(req);
    handleTokenSuccess(tokens, req.email, req.tenantId);
  };

  const signup = async (req: SignupRequest): Promise<void> => {
    const tokens = await authApi.signup(req);
    handleTokenSuccess(tokens, req.email, req.tenantId);
  };

  const googleLogin = async (idToken: string, tenantId: string): Promise<void> => {
    const tokens = await authApi.googleLogin({ idToken, tenantId });
    const decoded = parseJwt(tokens.accessToken);
    handleTokenSuccess(tokens, 'Google User', decoded?.tenant_id || tenantId);
  };

  const acceptInvite = async (req: AcceptInviteRequest): Promise<void> => {
    const tokens = await authApi.acceptInvite(req);
    handleTokenSuccess(tokens, 'Teammate', req.tenantId);
  };

  const logout = (): void => {
    clearAuthSession();
    setUser(null);
    setAccessToken(null);
  };

  const hasRole = (requiredRole: Role): boolean => {
    if (!user) return false;
    const userLevel = ROLE_HIERARCHY[user.role] ?? 0;
    const requiredLevel = ROLE_HIERARCHY[requiredRole] ?? 0;
    return userLevel >= requiredLevel;
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        accessToken,
        isAuthenticated: !!user && !!accessToken,
        isLoading,
        login,
        signup,
        googleLogin,
        acceptInvite,
        logout,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
