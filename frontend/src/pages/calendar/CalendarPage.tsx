import React, { useEffect, useState } from 'react';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Clock,
  FolderKanban,
  List,
  Plus,
  Trash2,
  Users,
  CheckSquare,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import {
  calendarApi,
  type CalendarEvent,
  type CreateCalendarEventRequest,
} from '../../api/calendarApi';
import { projectsApi, type ProjectResponse } from '../../api/projectsApi';
import { usersApi } from '../../api/usersApi';
import { hrmApi } from '../../api/hrmApi';
import type { WorkspaceUser, Employee } from '../../api/types';
import { wsManager } from '../../collaboration/websocketClient';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';

type CalendarViewMode = 'month' | 'agenda';

export const CalendarPage: React.FC = () => {
  const { user } = useAuth();

  const [currentDate, setCurrentDate] = useState(new Date());
  const [viewMode, setViewMode] = useState<CalendarViewMode>('month');
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [selectedDay, setSelectedDay] = useState<Date>(new Date());
  const [isLoading, setIsLoading] = useState(true);

  // Reference data
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [workspaceUsers, setWorkspaceUsers] = useState<WorkspaceUser[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);

  // Modal State
  const [isNewEventOpen, setIsNewEventOpen] = useState(false);
  const [eventTitle, setEventTitle] = useState('');
  const [eventDescription, setEventDescription] = useState('');
  const [eventStartDate, setEventStartDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [eventStartTime, setEventStartTime] = useState('09:00');
  const [eventEndDate, setEventEndDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [eventEndTime, setEventEndTime] = useState('10:00');
  const [isAllDay, setIsAllDay] = useState(false);
  const [selectedProjectId, setSelectedProjectId] = useState<string>('');
  const [selectedAttendeeIds, setSelectedAttendeeIds] = useState<string[]>([]);
  const [modalError, setModalError] = useState<string | null>(null);

  // Month navigation calculation
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December',
  ];

  useEffect(() => {
    let isMounted = true;
    const start = new Date(year, month - 1, 20).toISOString();
    const end = new Date(year, month + 2, 10).toISOString();

    calendarApi.getEvents({ from: start, to: end })
      .then((data) => {
        if (isMounted) {
          setEvents(data);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        console.error('Failed to load calendar events:', err);
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [year, month]);

  // Load projects & users for modal
  useEffect(() => {
    Promise.all([
      projectsApi.getProjects(),
      usersApi.getUsers(),
      hrmApi.getEmployees(),
    ])
      .then(([projList, uList, empList]) => {
        setProjects(projList);
        setWorkspaceUsers(uList.filter((u) => u.status === 'ACTIVE'));
        setEmployees(empList);
      })
      .catch((err) => console.error('Failed to load metadata:', err));
  }, []);

  // WebSocket real-time subscription
  useEffect(() => {
    const unsub = wsManager.subscribeToCalendar<CalendarEvent>((incoming) => {
      setEvents((prev) => {
        const exists = prev.some((e) => e.id === incoming.id);
        if (exists) {
          return prev.map((e) => (e.id === incoming.id ? incoming : e));
        }
        return [...prev, incoming];
      });
    });
    return () => unsub();
  }, []);

  // Navigation handlers
  const handlePrevMonth = () => {
    setCurrentDate(new Date(year, month - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentDate(new Date(year, month + 1, 1));
  };

  const handleToday = () => {
    const today = new Date();
    setCurrentDate(today);
    setSelectedDay(today);
  };

  // Create Event Handler
  const handleCreateEvent = async (e: React.FormEvent) => {
    e.preventDefault();
    setModalError(null);

    if (!eventTitle.trim()) {
      setModalError('Event title is required');
      return;
    }

    try {
      const startAt = isAllDay
        ? new Date(`${eventStartDate}T00:00:00Z`).toISOString()
        : new Date(`${eventStartDate}T${eventStartTime}:00Z`).toISOString();
      const endAt = isAllDay
        ? new Date(`${eventEndDate}T23:59:59Z`).toISOString()
        : new Date(`${eventEndDate}T${eventEndTime}:00Z`).toISOString();

      if (new Date(endAt).getTime() < new Date(startAt).getTime()) {
        setModalError('End time cannot be earlier than start time');
        return;
      }

      const req: CreateCalendarEventRequest = {
        title: eventTitle.trim(),
        description: eventDescription.trim() || undefined,
        startAt,
        endAt,
        allDay: isAllDay,
        projectId: selectedProjectId || undefined,
        attendeeUserIds: selectedAttendeeIds.length > 0 ? selectedAttendeeIds : undefined,
      };

      const created = await calendarApi.createEvent(req);
      setEvents((prev) => [...prev, created]);
      setIsNewEventOpen(false);

      // Reset form
      setEventTitle('');
      setEventDescription('');
      setSelectedProjectId('');
      setSelectedAttendeeIds([]);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to create event';
      setModalError(msg);
    }
  };

  // Delete Event Handler
  const handleDeleteEvent = async (id: string) => {
    if (!window.confirm('Delete this event?')) return;
    try {
      await calendarApi.deleteEvent(id);
      setEvents((prev) => prev.filter((e) => e.id !== id));
    } catch (err) {
      console.error('Failed to delete event:', err);
    }
  };

  // Calendar Grid Builder
  const firstDayIndex = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const daysInPrevMonth = new Date(year, month, 0).getDate();

  const calendarDays = [];

  // Previous month overflow days
  for (let i = firstDayIndex - 1; i >= 0; i--) {
    const d = new Date(year, month - 1, daysInPrevMonth - i);
    calendarDays.push({ date: d, isCurrentMonth: false });
  }

  // Current month days
  for (let d = 1; d <= daysInMonth; d++) {
    const dateObj = new Date(year, month, d);
    calendarDays.push({ date: dateObj, isCurrentMonth: true });
  }

  // Next month overflow days (fill grid to multiple of 7)
  const remainingDays = 42 - calendarDays.length;
  for (let d = 1; d <= remainingDays; d++) {
    const dateObj = new Date(year, month + 1, d);
    calendarDays.push({ date: dateObj, isCurrentMonth: false });
  }

  // Date utilities
  const isSameDay = (d1: Date, d2: Date) =>
    d1.getFullYear() === d2.getFullYear() &&
    d1.getMonth() === d2.getMonth() &&
    d1.getDate() === d2.getDate();

  const isToday = (d: Date) => isSameDay(d, new Date());

  const getEventsForDay = (day: Date) => {
    return events.filter((e) => {
      const eventDate = new Date(e.startAt);
      return isSameDay(eventDate, day);
    });
  };

  const selectedDayEvents = getEventsForDay(selectedDay);

  const getUserDisplayName = (u: WorkspaceUser) => {
    const emp = employees.find((e) => e.email.toLowerCase() === u.email.toLowerCase());
    return emp?.name || u.email.split('@')[0];
  };

  const formatEventTime = (e: CalendarEvent) => {
    if (e.allDay) return 'All Day';
    try {
      const start = new Date(e.startAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const end = new Date(e.endAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      return `${start} - ${end}`;
    } catch {
      return '';
    }
  };

  return (
    <div className="space-y-4">
      {/* Top Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100 flex items-center gap-2">
            <CalendarIcon className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            Calendar & Deadlines
          </h1>
          <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-0.5">
            Internal company events, project delivery deadlines, and task milestones.
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {/* View Toggle */}
          <div className="flex items-center rounded-lg border border-neutral-200 dark:border-[#262626] p-0.5 bg-neutral-50 dark:bg-[#141414]">
            <button
              onClick={() => setViewMode('month')}
              className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${
                viewMode === 'month'
                  ? 'bg-white dark:bg-[#222] text-neutral-900 dark:text-white shadow-2xs'
                  : 'text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-200'
              }`}
            >
              Month
            </button>
            <button
              onClick={() => setViewMode('agenda')}
              className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors flex items-center gap-1.5 ${
                viewMode === 'agenda'
                  ? 'bg-white dark:bg-[#222] text-neutral-900 dark:text-white shadow-2xs'
                  : 'text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-200'
              }`}
            >
              <List className="w-3.5 h-3.5" /> Agenda
            </button>
          </div>

          {/* Month Navigation */}
          <div className="flex items-center gap-1 bg-white dark:bg-[#141414] border border-neutral-200 dark:border-[#262626] rounded-lg p-0.5">
            <button
              onClick={handlePrevMonth}
              className="p-1.5 text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-200 rounded-md hover:bg-neutral-100 dark:hover:bg-[#202020] cursor-pointer"
              aria-label="Previous Month"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={handleToday}
              className="px-2 py-1 text-xs font-medium text-neutral-700 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-[#202020] rounded-md"
            >
              Today
            </button>
            <button
              onClick={handleNextMonth}
              className="p-1.5 text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-200 rounded-md hover:bg-neutral-100 dark:hover:bg-[#202020] cursor-pointer"
              aria-label="Next Month"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          <span className="text-sm font-semibold text-neutral-800 dark:text-neutral-200 min-w-32 text-center">
            {monthNames[month]} {year}
          </span>

          <Button
            size="sm"
            onClick={() => setIsNewEventOpen(true)}
            leftIcon={<Plus className="w-3.5 h-3.5" />}
          >
            New Event
          </Button>
        </div>
      </div>

      {isLoading ? (
        <LoadingSkeleton variant="table" rows={6} />
      ) : viewMode === 'month' ? (
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
          {/* ----------------------------------------------------------- */}
          {/* MONTH CALENDAR GRID                                         */}
          {/* ----------------------------------------------------------- */}
          <div className="lg:col-span-3 border border-neutral-200 dark:border-[#262626] rounded-xl overflow-hidden bg-white dark:bg-[#101010] shadow-2xs">
            {/* Days of Week Header */}
            <div className="grid grid-cols-7 border-b border-neutral-200 dark:border-[#262626] bg-neutral-50/70 dark:bg-[#141414] text-center text-[11px] font-semibold text-neutral-400 py-2">
              <span>SUN</span>
              <span>MON</span>
              <span>TUE</span>
              <span>WED</span>
              <span>THU</span>
              <span>FRI</span>
              <span>SAT</span>
            </div>

            {/* Day Cells */}
            <div className="grid grid-cols-7 auto-rows-fr divide-x divide-y divide-neutral-200 dark:divide-[#262626]">
              {calendarDays.map((item, idx) => {
                const dayEvents = getEventsForDay(item.date);
                const selected = isSameDay(item.date, selectedDay);
                const today = isToday(item.date);

                return (
                  <div
                    key={idx}
                    onClick={() => setSelectedDay(item.date)}
                    className={`min-h-24 p-1.5 transition-colors cursor-pointer flex flex-col justify-between ${
                      !item.isCurrentMonth
                        ? 'bg-neutral-50/40 dark:bg-[#0c0c0c]/40 text-neutral-400'
                        : 'bg-white dark:bg-[#101010] text-neutral-800 dark:text-neutral-200'
                    } ${
                      selected
                        ? 'ring-2 ring-blue-500/80 bg-blue-50/20 dark:bg-blue-950/20 z-10'
                        : 'hover:bg-neutral-50 dark:hover:bg-[#181818]'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span
                        className={`text-xs font-medium h-5 w-5 flex items-center justify-center rounded-full ${
                          today
                            ? 'bg-neutral-900 text-white dark:bg-white dark:text-neutral-900 font-bold'
                            : ''
                        }`}
                      >
                        {item.date.getDate()}
                      </span>
                      {dayEvents.length > 0 && (
                        <span className="text-[10px] text-neutral-400 font-mono">
                          {dayEvents.length}
                        </span>
                      )}
                    </div>

                    {/* Chips */}
                    <div className="space-y-1 overflow-hidden">
                      {dayEvents.slice(0, 3).map((e) => {
                        const isDeadline = e.type === 'PROJECT_DEADLINE';
                        const isTaskDue = e.type === 'TASK_DUE';
                        return (
                          <div
                            key={e.id}
                            className={`text-[10px] px-1.5 py-0.5 rounded truncate font-medium flex items-center gap-1 ${
                              isDeadline
                                ? 'bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300 border border-rose-200 dark:border-rose-900/40'
                                : isTaskDue
                                ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-200 dark:border-amber-900/40'
                                : 'bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300 border border-blue-200 dark:border-blue-900/40'
                            }`}
                            title={e.title}
                          >
                            {isDeadline && <FolderKanban className="w-2.5 h-2.5 shrink-0" />}
                            {isTaskDue && <CheckSquare className="w-2.5 h-2.5 shrink-0" />}
                            <span className="truncate">{e.title}</span>
                          </div>
                        );
                      })}
                      {dayEvents.length > 3 && (
                        <span className="text-[10px] text-neutral-400 font-medium pl-1">
                          +{dayEvents.length - 3} more
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* ----------------------------------------------------------- */}
          {/* DAY AGENDA SIDE SHEET                                       */}
          {/* ----------------------------------------------------------- */}
          <div className="border border-neutral-200 dark:border-[#262626] rounded-xl p-4 bg-white dark:bg-[#101010] shadow-2xs flex flex-col">
            <div className="border-b border-neutral-200 dark:border-[#262626] pb-3 mb-3">
              <span className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wider block">
                Selected Date
              </span>
              <h3 className="text-base font-bold text-neutral-900 dark:text-neutral-100 mt-0.5">
                {selectedDay.toLocaleDateString([], {
                  weekday: 'short',
                  month: 'short',
                  day: 'numeric',
                  year: 'numeric',
                })}
              </h3>
            </div>

            <div className="flex-1 overflow-y-auto space-y-3">
              {selectedDayEvents.length === 0 ? (
                <div className="py-8 text-center text-xs text-neutral-400">
                  <CalendarIcon className="w-8 h-8 mx-auto mb-2 opacity-30" />
                  No events or deadlines for this day.
                </div>
              ) : (
                selectedDayEvents.map((event) => (
                  <div
                    key={event.id}
                    className="p-3 rounded-lg border border-neutral-200 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#181818] space-y-2 text-xs"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="flex items-center gap-1.5 flex-wrap">
                          <span className="font-semibold text-neutral-900 dark:text-neutral-100">
                            {event.title}
                          </span>
                          <Badge
                            variant={
                              event.type === 'PROJECT_DEADLINE'
                                ? 'danger'
                                : event.type === 'TASK_DUE'
                                ? 'warning'
                                : 'default'
                            }
                            size="sm"
                          >
                            {event.type.replace('_', ' ')}
                          </Badge>
                        </div>
                        <div className="flex items-center gap-1 text-[11px] text-neutral-400 mt-1">
                          <Clock className="w-3 h-3" />
                          <span>{formatEventTime(event)}</span>
                        </div>
                      </div>

                      {event.type === 'CUSTOM' && (
                        <button
                          onClick={() => handleDeleteEvent(event.id)}
                          className="text-neutral-400 hover:text-rose-600 dark:hover:text-rose-400 p-1 rounded cursor-pointer"
                          title="Delete event"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>

                    {event.description && (
                      <p className="text-neutral-600 dark:text-neutral-400 text-[11px] leading-relaxed">
                        {event.description}
                      </p>
                    )}

                    {event.projectName && (
                      <div className="flex items-center gap-1 text-[11px] text-blue-600 dark:text-blue-400">
                        <FolderKanban className="w-3 h-3" />
                        <span>Project: {event.projectName}</span>
                      </div>
                    )}

                    {event.taskTitle && (
                      <div className="flex items-center gap-1 text-[11px] text-amber-600 dark:text-amber-400">
                        <CheckSquare className="w-3 h-3" />
                        <span>Task: {event.taskTitle}</span>
                      </div>
                    )}

                    {event.attendees && event.attendees.length > 0 && (
                      <div className="pt-1 border-t border-neutral-200 dark:border-[#262626]">
                        <span className="text-[10px] text-neutral-400 block mb-1">
                          Attendees ({event.attendees.length}):
                        </span>
                        <div className="flex flex-wrap gap-1">
                          {event.attendees.map((a) => (
                            <span
                              key={a.userId}
                              className="px-1.5 py-0.5 rounded bg-neutral-200 dark:bg-[#2a2a2a] text-[10px] text-neutral-700 dark:text-neutral-300"
                            >
                              {a.name}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      ) : (
        /* ------------------------------------------------------------- */
        /* AGENDA LIST VIEW                                              */
        /* ------------------------------------------------------------- */
        <div className="border border-neutral-200 dark:border-[#262626] rounded-xl bg-white dark:bg-[#101010] p-4 shadow-2xs divide-y divide-neutral-200 dark:divide-[#262626]">
          {events.length === 0 ? (
            <div className="py-12 text-center text-sm text-neutral-400">
              No events found for this period.
            </div>
          ) : (
            events.map((event) => (
              <div key={event.id} className="py-3 flex items-start justify-between gap-4 text-xs">
                <div className="space-y-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-neutral-900 dark:text-neutral-100 text-sm">
                      {event.title}
                    </span>
                    <Badge
                      variant={
                        event.type === 'PROJECT_DEADLINE'
                          ? 'danger'
                          : event.type === 'TASK_DUE'
                          ? 'warning'
                          : 'default'
                      }
                      size="sm"
                    >
                      {event.type.replace('_', ' ')}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-3 text-neutral-400 text-[11px]">
                    <span>
                      {new Date(event.startAt).toLocaleDateString([], {
                        month: 'short',
                        day: 'numeric',
                        year: 'numeric',
                      })}
                    </span>
                    <span>•</span>
                    <span>{formatEventTime(event)}</span>
                    {event.projectName && (
                      <>
                        <span>•</span>
                        <span className="text-blue-500">Project: {event.projectName}</span>
                      </>
                    )}
                  </div>
                  {event.description && (
                    <p className="text-neutral-600 dark:text-neutral-400 text-xs">
                      {event.description}
                    </p>
                  )}
                </div>

                {event.type === 'CUSTOM' && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDeleteEvent(event.id)}
                    className="text-neutral-400 hover:text-rose-600 h-8 w-8 !p-0 shrink-0"
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                )}
              </div>
            ))
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* MODAL: Create New Event                                       */}
      {/* ------------------------------------------------------------- */}
      <Modal
        isOpen={isNewEventOpen}
        onClose={() => setIsNewEventOpen(false)}
        title="Schedule Event"
        description="Add a meeting or reminder to the company workspace calendar."
      >
        <form onSubmit={handleCreateEvent} className="space-y-4 text-xs">
          {modalError && (
            <div className="p-2.5 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/50 text-rose-600 dark:text-rose-400">
              {modalError}
            </div>
          )}

          <div>
            <label className="block font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
              Event Title
            </label>
            <Input
              value={eventTitle}
              onChange={(e) => setEventTitle(e.target.value)}
              placeholder="e.g. Q3 Roadmap Review"
              className="text-xs"
              required
            />
          </div>

          <div>
            <label className="block font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
              Description (Optional)
            </label>
            <textarea
              value={eventDescription}
              onChange={(e) => setEventDescription(e.target.value)}
              placeholder="Add details, agenda, or meeting links..."
              rows={2}
              className="w-full rounded-lg border border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#141414] p-2 text-xs text-neutral-900 dark:text-neutral-100 focus:outline-hidden"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
                Start Date
              </label>
              <Input
                type="date"
                value={eventStartDate}
                onChange={(e) => setEventStartDate(e.target.value)}
                className="text-xs"
                required
              />
            </div>
            {!isAllDay && (
              <div>
                <label className="block font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
                  Start Time
                </label>
                <Input
                  type="time"
                  value={eventStartTime}
                  onChange={(e) => setEventStartTime(e.target.value)}
                  className="text-xs"
                />
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
                End Date
              </label>
              <Input
                type="date"
                value={eventEndDate}
                onChange={(e) => setEventEndDate(e.target.value)}
                className="text-xs"
                required
              />
            </div>
            {!isAllDay && (
              <div>
                <label className="block font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
                  End Time
                </label>
                <Input
                  type="time"
                  value={eventEndTime}
                  onChange={(e) => setEventEndTime(e.target.value)}
                  className="text-xs"
                />
              </div>
            )}
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="allDayCheck"
              checked={isAllDay}
              onChange={(e) => setIsAllDay(e.target.checked)}
              className="rounded border-neutral-300 text-blue-600 cursor-pointer"
            />
            <label htmlFor="allDayCheck" className="text-neutral-700 dark:text-neutral-300 cursor-pointer">
              All Day Event
            </label>
          </div>

          <div>
            <label className="block font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
              Associate with Project (Optional)
            </label>
            <select
              value={selectedProjectId}
              onChange={(e) => setSelectedProjectId(e.target.value)}
              className="w-full rounded-lg border border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#141414] p-2 text-xs text-neutral-900 dark:text-neutral-100"
            >
              <option value="">None (Workspace General)</option>
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
              Invite Teammates
            </label>
            <div className="max-h-36 overflow-y-auto border border-neutral-200 dark:border-[#262626] rounded-lg p-2 space-y-1">
              {workspaceUsers
                .filter((u) => u.id !== user?.id)
                .map((u) => {
                  const isChecked = selectedAttendeeIds.includes(u.id);
                  return (
                    <label
                      key={u.id}
                      className="flex items-center justify-between p-1 rounded hover:bg-neutral-50 dark:hover:bg-[#1e1e1e] cursor-pointer"
                    >
                      <div className="flex items-center gap-2">
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => {
                            setSelectedAttendeeIds((prev) =>
                              isChecked ? prev.filter((id) => id !== u.id) : [...prev, u.id]
                            );
                          }}
                          className="rounded border-neutral-300 text-blue-600"
                        />
                        <span>{getUserDisplayName(u)}</span>
                      </div>
                      <Badge variant="default" size="sm">
                        {u.role}
                      </Badge>
                    </label>
                  );
                })}
              {workspaceUsers.filter((u) => u.id !== user?.id).length === 0 && (
                <div className="text-neutral-400 text-center py-2">
                  <Users className="w-4 h-4 mx-auto mb-1 opacity-40" />
                  No other active teammates to invite.
                </div>
              )}
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsNewEventOpen(false)}
            >
              Cancel
            </Button>
            <Button type="submit" size="sm">
              Schedule Event
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
