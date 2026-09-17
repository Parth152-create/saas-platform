export interface AppNotification {
  id: string;
  title: string;
  description?: string;
  timestamp: string;
  createdAt: string;
  read: boolean;
  category?: 'employee' | 'task' | 'claim' | 'billing' | 'system';
  targetPath?: string;
}
