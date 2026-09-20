import React, { createContext, useContext, useEffect, useState } from 'react';
import type { AppNotification } from '../types/notification';
import {
  notificationsApi,
  saveStoredNotifications,
  mapToAppNotification,
} from '../api/notificationsApi';
import { INITIAL_NOTIFICATIONS } from '../mocks/mockNotifications';
import { useAuth } from './AuthContext';
import { wsManager } from '../collaboration/websocketClient';

export interface NotificationContextType {
  notifications: AppNotification[];
  unreadCount: number;
  isLoading: boolean;
  error: string | null;
  markAsRead: (id: string) => Promise<void>;
  markAllAsRead: () => Promise<void>;
  dismissNotification: (id: string) => void;
  resetNotifications: () => void;
  refetch: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isLiveBackend, setIsLiveBackend] = useState<boolean>(false);

  useEffect(() => {
    let isCancelled = false;

    if (!isAuthenticated) {
      const timer = setTimeout(() => {
        if (!isCancelled) {
          setNotifications([]);
          setIsLoading(false);
          setError(null);
        }
      }, 0);
      return () => {
        isCancelled = true;
        clearTimeout(timer);
      };
    }

    const fetchInitial = async () => {
      try {
        const result = await notificationsApi.fetchNotifications();
        if (!isCancelled) {
          setNotifications(result.data);
          setIsLiveBackend(result.isLiveBackend);
        }
      } catch {
        if (!isCancelled) {
          setError('Unable to load notifications.');
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    fetchInitial();

    // Connect WebSocket for live notifications
    wsManager.connect();
    const unsubWs = wsManager.subscribeToNotifications<{
      id: string;
      userId?: string;
      title: string;
      message?: string;
      type?: string;
      targetUrl?: string;
      read: boolean;
      createdAt: string;
    }>((notifData) => {
      if (!isCancelled) {
        const appNotif = mapToAppNotification(notifData);
        setNotifications((prev) => [appNotif, ...prev.filter((n) => n.id !== appNotif.id)]);
      }
    });

    return () => {
      isCancelled = true;
      unsubWs();
    };
  }, [isAuthenticated]);

  const refetch = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await notificationsApi.fetchNotifications();
      setNotifications(result.data);
      setIsLiveBackend(result.isLiveBackend);
    } catch {
      setError('Unable to load notifications.');
    } finally {
      setIsLoading(false);
    }
  };

  const markAsRead = async (id: string) => {
    setNotifications((prev) => {
      const updated = prev.map((n) => (n.id === id ? { ...n, read: true } : n));
      if (!isLiveBackend) {
        saveStoredNotifications(updated);
      }
      return updated;
    });
    await notificationsApi.markAsRead(id, isLiveBackend);
  };

  const markAllAsRead = async () => {
    setNotifications((prev) => {
      const updated = prev.map((n) => ({ ...n, read: true }));
      if (!isLiveBackend) {
        saveStoredNotifications(updated);
      }
      return updated;
    });
    await notificationsApi.markAllAsRead(isLiveBackend);
  };

  const dismissNotification = (id: string) => {
    setNotifications((prev) => {
      const updated = prev.filter((n) => n.id !== id);
      if (!isLiveBackend) {
        saveStoredNotifications(updated);
      }
      return updated;
    });
  };

  const resetNotifications = () => {
    setNotifications(INITIAL_NOTIFICATIONS);
    saveStoredNotifications(INITIAL_NOTIFICATIONS);
    setError(null);
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        isLoading,
        error,
        markAsRead,
        markAllAsRead,
        dismissNotification,
        resetNotifications,
        refetch,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};

export function useNotifications(): NotificationContextType {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
}
