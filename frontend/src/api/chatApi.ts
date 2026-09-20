import { apiClient } from './apiClient';

export type ChannelType = 'WORKSPACE' | 'PROJECT';

export interface ChatChannel {
  id: string;
  name: string;
  topic: string | null;
  type: ChannelType;
  projectId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: string;
  channelId: string;
  senderId: string;
  senderName: string;
  senderEmail: string;
  content: string;
  createdAt: string;
}

export interface DirectConversation {
  id: string;
  otherUserId: string;
  otherUserName: string;
  otherUserEmail: string;
  otherUserRole: string;
  lastMessage: string | null;
  lastMessageAt: string;
  unreadCount: number;
  createdAt: string;
}

export interface DirectMessage {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  senderEmail: string;
  content: string;
  readAt: string | null;
  createdAt: string;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CreateChannelData {
  name: string;
  topic?: string;
}

export const chatApi = {
  getChannels: () => apiClient.get<ChatChannel[]>('/api/chat/channels'),

  createChannel: (data: CreateChannelData) =>
    apiClient.post<ChatChannel>('/api/chat/channels', data),

  deleteChannel: (channelId: string) =>
    apiClient.delete<void>(`/api/chat/channels/${channelId}`),

  getOrCreateProjectChannel: (projectId: string) =>
    apiClient.get<ChatChannel>(`/api/chat/projects/${projectId}/channel`),

  getChannelMessages: (channelId: string, page = 0, size = 50) =>
    apiClient.get<SpringPage<ChatMessage>>(
      `/api/chat/channels/${channelId}/messages?page=${page}&size=${size}`
    ),

  sendChannelMessage: (channelId: string, content: string) =>
    apiClient.post<ChatMessage>(`/api/chat/channels/${channelId}/messages`, { content }),

  getDirectConversations: () =>
    apiClient.get<DirectConversation[]>('/api/chat/direct'),

  getOrCreateDirectConversation: (userId: string) =>
    apiClient.post<DirectConversation>(`/api/chat/direct/${userId}`),

  getDirectMessages: (conversationId: string, page = 0, size = 50) =>
    apiClient.get<SpringPage<DirectMessage>>(
      `/api/chat/direct/${conversationId}/messages?page=${page}&size=${size}`
    ),

  sendDirectMessage: (conversationId: string, content: string) =>
    apiClient.post<DirectMessage>(`/api/chat/direct/${conversationId}/messages`, { content }),
};
