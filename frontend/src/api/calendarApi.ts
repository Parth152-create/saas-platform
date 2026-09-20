import { apiClient } from './apiClient';

export type CalendarEventType = 'CUSTOM' | 'TASK_DUE' | 'PROJECT_DEADLINE';

export interface CalendarAttendee {
  userId: string;
  name: string;
  email: string;
}

export interface CalendarEvent {
  id: string;
  title: string;
  description: string | null;
  startAt: string;
  endAt: string;
  allDay: boolean;
  type: CalendarEventType;
  projectId: string | null;
  projectName: string | null;
  taskId: string | null;
  taskTitle: string | null;
  createdById: string | null;
  attendees: CalendarAttendee[];
  createdAt: string;
}

export interface CreateCalendarEventRequest {
  title: string;
  description?: string;
  startAt: string;
  endAt: string;
  allDay?: boolean;
  projectId?: string;
  attendeeUserIds?: string[];
}

export interface UpdateCalendarEventRequest {
  title?: string;
  description?: string;
  startAt?: string;
  endAt?: string;
  allDay?: boolean;
  projectId?: string;
  attendeeUserIds?: string[];
}

export const calendarApi = {
  getEvents: (params?: { from?: string; to?: string; projectId?: string }) => {
    const query = new URLSearchParams();
    if (params?.from) query.append('from', params.from);
    if (params?.to) query.append('to', params.to);
    if (params?.projectId) query.append('projectId', params.projectId);
    const queryString = query.toString();
    return apiClient.get<CalendarEvent[]>(`/api/calendar${queryString ? `?${queryString}` : ''}`);
  },

  getEvent: (id: string) =>
    apiClient.get<CalendarEvent>(`/api/calendar/${id}`),

  createEvent: (data: CreateCalendarEventRequest) =>
    apiClient.post<CalendarEvent>('/api/calendar', data),

  updateEvent: (id: string, data: UpdateCalendarEventRequest) =>
    apiClient.put<CalendarEvent>(`/api/calendar/${id}`, data),

  deleteEvent: (id: string) =>
    apiClient.delete<void>(`/api/calendar/${id}`),
};
