import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { billingApi } from '../api/billingApi';
import type { Feature, FeatureEntitlementsResponse } from '../api/types';
import { useAuth } from './AuthContext';

interface EntitlementsContextType {
  entitlements: FeatureEntitlementsResponse | null;
  plan: string;
  status: string;
  features: Feature[];
  isLoading: boolean;
  error: string | null;
  hasFeature: (feature: Feature) => boolean;
  refetchEntitlements: () => Promise<void>;
}

const EntitlementsContext = createContext<EntitlementsContextType | undefined>(undefined);

export const EntitlementsProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, user } = useAuth();
  const [entitlements, setEntitlements] = useState<FeatureEntitlementsResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    if (!isAuthenticated) {
      Promise.resolve().then(() => {
        if (isMounted) {
          setEntitlements(null);
          setIsLoading(false);
        }
      });
      return () => {
        isMounted = false;
      };
    }
    billingApi
      .getEntitlements()
      .then((data) => {
        if (isMounted) {
          setEntitlements(data);
          setError(null);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const message = err instanceof Error ? err.message : 'Failed to load feature entitlements';
          setError(message);
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, user?.tenantId]);

  const refetchEntitlements = useCallback(async () => {
    if (!isAuthenticated) {
      setEntitlements(null);
      return;
    }
    setIsLoading(true);
    try {
      const data = await billingApi.getEntitlements();
      setEntitlements(data);
      setError(null);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load feature entitlements';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  const hasFeature = useCallback(
    (feature: Feature): boolean => {
      if (!entitlements || !entitlements.features) return false;
      return entitlements.features.includes(feature);
    },
    [entitlements]
  );

  const plan = entitlements?.plan || 'STARTER';
  const status = entitlements?.status || 'ACTIVE';
  const features = entitlements?.features || [];

  return (
    <EntitlementsContext.Provider
      value={{
        entitlements,
        plan,
        status,
        features,
        isLoading,
        error,
        hasFeature,
        refetchEntitlements,
      }}
    >
      {children}
    </EntitlementsContext.Provider>
  );
};

export function useEntitlements(): EntitlementsContextType {
  const context = useContext(EntitlementsContext);
  if (!context) {
    throw new Error('useEntitlements must be used within an EntitlementsProvider');
  }
  return context;
}
