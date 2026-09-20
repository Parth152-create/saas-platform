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

interface BackendNotificationItem {
  id: string;
  userId?: string;
  title: string;
  message?: string;
  description?: string;
  type?: string;
  targetUrl?: string;
  targetPath?: string;
  read: boolean;
  createdAt: string;
}

export function mapToAppNotification(item: BackendNotificationItem): AppNotification {
  const mapCategory = (type?: string): 'employee' | 'task' | 'claim' | 'billing' | 'system' => {
    if (!type) return 'system';
    if (type.includes('TASK')) return 'task';
    if (type.includes('PROJECT') || type.includes('MEMBER')) return 'employee';
    if (type.includes('BILLING') || type.includes('INVOICE')) return 'billing';
    if (type.includes('CLAIM')) return 'claim';
    return 'system';
  };

  return {
    id: item.id,
    title: item.title,
    description: item.message || item.description || '',
    timestamp: item.createdAt,
    createdAt: item.createdAt,
    read: item.read,
    category: mapCategory(item.type),
    targetPath: item.targetUrl || item.targetPath,
  };
}

export const notificationsApi = {
  fetchNotifications: async (): Promise<{ data: AppNotification[]; isLiveBackend: boolean }> => {
    try {
      const response = await apiClient.get<unknown>('/api/notifications');
      let items: BackendNotificationItem[] = [];
      if (Array.isArray(response)) {
        items = response as BackendNotificationItem[];
      } else if (response && typeof response === 'object' && 'content' in response) {
        items = (response as { content: BackendNotificationItem[] }).content;
      }
      const mapped = items.map(mapToAppNotification);
      return { data: mapped, isLiveBackend: true };
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

  getUnreadCount: async (): Promise<number> => {
    try {
      const res = await apiClient.get<{ count: number }>('/api/notifications/unread-count');
      return res.count;
    } catch {
      return 0;
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
