import { Client, type StompSubscription } from '@stomp/stompjs';
import { getStoredAccessToken, getStoredUser } from '../api/apiClient';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090';

function getWebSocketUrl(): string {
  const wsUrl = API_BASE_URL.replace(/^http/, 'ws');
  return `${wsUrl}/ws`;
}

class WebSocketManager {
  private client: Client | null = null;
  private isConnecting = false;
  private isConnected = false;
  private connectionSubscribers = new Set<(connected: boolean) => void>();
  private activeSubscriptions = new Map<string, StompSubscription>();

  public connect(): void {
    if (this.isConnected || this.isConnecting) return;

    const token = getStoredAccessToken();
    if (!token) return;

    this.isConnecting = true;

    this.client = new Client({
      brokerURL: getWebSocketUrl(),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.isConnected = true;
        this.isConnecting = false;
        this.notifySubscribers(true);
      },
      onDisconnect: () => {
        this.isConnected = false;
        this.isConnecting = false;
        this.notifySubscribers(false);
      },
      onStompError: (frame) => {
        console.warn('WebSocket STOMP error:', frame.headers['message'], frame.body);
        this.isConnected = false;
        this.isConnecting = false;
        this.notifySubscribers(false);
      },
      onWebSocketClose: () => {
        this.isConnected = false;
        this.isConnecting = false;
        this.notifySubscribers(false);
      },
    });

    try {
      this.client.activate();
    } catch (err) {
      console.error('Failed to activate STOMP client:', err);
      this.isConnecting = false;
    }
  }

  public disconnect(): void {
    this.activeSubscriptions.forEach((sub) => {
      try {
        sub.unsubscribe();
      } catch {
        // Ignore unsubscribe error on disconnect
      }
    });
    this.activeSubscriptions.clear();

    if (this.client) {
      try {
        this.client.deactivate();
      } catch {
        // Ignore deactivate error
      }
      this.client = null;
    }
    this.isConnected = false;
    this.isConnecting = false;
    this.notifySubscribers(false);
  }

  public onConnectionChange(cb: (connected: boolean) => void): () => void {
    this.connectionSubscribers.add(cb);
    cb(this.isConnected);
    return () => {
      this.connectionSubscribers.delete(cb);
    };
  }

  private notifySubscribers(connected: boolean): void {
    this.connectionSubscribers.forEach((cb) => {
      try {
        cb(connected);
      } catch {
        // Ignore subscriber callback errors
      }
    });
  }

  public subscribe<T>(destination: string, onMessage: (data: T) => void): () => void {
    const doSubscribe = () => {
      if (!this.client || !this.isConnected) return null;
      try {
        const sub = this.client.subscribe(destination, (frame) => {
          try {
            const parsed = JSON.parse(frame.body) as T;
            onMessage(parsed);
          } catch (e) {
            console.error('Failed to parse WebSocket message from destination:', destination, e);
          }
        });
        return sub;
      } catch (err) {
        console.error('Failed to subscribe to destination:', destination, err);
        return null;
      }
    };

    let sub = doSubscribe();

    const unsubConnectionChange = this.onConnectionChange((connected) => {
      if (connected && !sub) {
        sub = doSubscribe();
      }
    });

    return () => {
      unsubConnectionChange();
      if (sub) {
        try {
          sub.unsubscribe();
        } catch {
          // Ignore unsubscribe error
        }
      }
    };
  }

  public subscribeToChannelMessage<T>(channelId: string, onMessage: (msg: T) => void): () => void {
    const user = getStoredUser();
    if (!user?.tenantId) return () => {};
    const destination = `/topic/tenant/${user.tenantId}/channels/${channelId}`;
    return this.subscribe<T>(destination, onMessage);
  }

  public subscribeToDirectMessage<T>(conversationId: string, onMessage: (msg: T) => void): () => void {
    const user = getStoredUser();
    if (!user?.tenantId) return () => {};
    const destination = `/topic/tenant/${user.tenantId}/dm/${conversationId}`;
    return this.subscribe<T>(destination, onMessage);
  }

  public subscribeToNotifications<T>(onNotification: (notif: T) => void): () => void {
    const user = getStoredUser();
    if (!user?.tenantId || !user?.id) return () => {};
    const destination = `/topic/tenant/${user.tenantId}/users/${user.id}/notifications`;
    return this.subscribe<T>(destination, onNotification);
  }

  public subscribeToCalendar<T>(onEvent: (event: T) => void): () => void {
    const user = getStoredUser();
    if (!user?.tenantId) return () => {};
    const destination = `/topic/tenant/${user.tenantId}/calendar`;
    return this.subscribe<T>(destination, onEvent);
  }
}

export const wsManager = new WebSocketManager();
