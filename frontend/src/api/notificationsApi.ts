import { apiClient, ApiError } from './apiClient';
import type { AppNotification } from '../types/notification';
import { INITIAL_NOTIFICATIONS } from '../mocks/mockNotifications';

const NOTIFICATIONS_STORAGE_KEY = 'saas_notifications_data';

export const getStoredNotifications = (): AppNotification[] => {
  try {
    const raw = localStorage.getItem(NOTIFICATIONS_STORAGE_KEY);
    if (!raw) return INITIAL_NOTIFICATIONS;
    return JSON.parse(raw) as AppNotification[];
  } catch {
    return INITIAL_NOTIFICATIONS;
  }
};

export const saveStoredNotifications = (notifications: AppNotification[]): void => {
  try {
    localStorage.setItem(NOTIFICATIONS_STORAGE_KEY, JSON.stringify(notifications));
  } catch {
    // Ignore storage write failures in private/sandboxed mode
  }
};

export const notificationsApi = {
  fetchNotifications: async (): Promise<{ data: AppNotification[]; isLiveBackend: boolean }> => {
    try {
      const liveData = await apiClient.get<AppNotification[]>('/api/notifications');
      return { data: liveData, isLiveBackend: true };
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        // 404 Not Found or 405/501 means no notification controller is mounted on the backend
        if (err.status === 404 || err.status === 405 || err.status === 501) {
          return { data: getStoredNotifications(), isLiveBackend: false };
        }
        // 401 Unauthorized propagates so that authentication handling handles expired session
        if (err.status === 401) {
          throw err;
        }
      }
      // Re-throw other errors (e.g. 500 or network failure) to display the error state
      throw err;
    }
  },

  markAsRead: async (id: string, isLiveBackend: boolean): Promise<void> => {
    if (isLiveBackend) {
      try {
        await apiClient.put(`/api/notifications/${id}/read`);
      } catch (err) {
        console.warn('Backend markAsRead call failed:', err);
      }
    }
  },

  markAllAsRead: async (isLiveBackend: boolean): Promise<void> => {
    if (isLiveBackend) {
      try {
        await apiClient.put('/api/notifications/read-all');
      } catch (err) {
        console.warn('Backend markAllAsRead call failed:', err);
      }
    }
  },
};
