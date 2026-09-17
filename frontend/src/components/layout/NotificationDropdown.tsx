import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, Bell, Check, RefreshCw, X } from 'lucide-react';
import { useNotifications } from '../../context/NotificationContext';
import type { AppNotification } from '../../types/notification';

export const NotificationDropdown: React.FC = () => {
  const {
    notifications,
    unreadCount,
    isLoading,
    error,
    markAsRead,
    markAllAsRead,
    dismissNotification,
    refetch,
  } = useNotifications();

  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  // Handle outside click and Escape key to close
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  const handleNotificationClick = async (notification: AppNotification) => {
    if (!notification.read) {
      await markAsRead(notification.id);
    }
    setIsOpen(false);
    if (notification.targetPath) {
      navigate(notification.targetPath);
    }
  };

  const handleMarkAllAsRead = async () => {
    if (unreadCount > 0) {
      await markAllAsRead();
    }
  };

  return (
    <div className="relative inline-block text-left" ref={containerRef}>
      {/* Bell Button */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 rounded-lg text-neutral-500 hover:bg-neutral-100 hover:text-neutral-800 dark:text-neutral-400 dark:hover:bg-[#1a1a1a] dark:hover:text-neutral-100 transition-colors cursor-pointer"
        aria-label="Notifications"
        aria-expanded={isOpen}
        aria-haspopup="dialog"
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-neutral-900 px-1 text-[10px] font-bold text-white dark:bg-white dark:text-[#0a0a0a] shadow-xs animate-in zoom-in-50">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Popover Panel */}
      {isOpen && (
        <div
          role="dialog"
          aria-label="Notifications"
          className="fixed left-3 right-3 top-16 sm:absolute sm:left-auto sm:right-0 sm:top-full sm:mt-2 sm:w-96 rounded-2xl border border-neutral-200 bg-white shadow-2xl transition-all animate-in fade-in zoom-in-95 dark:border-[#262626] dark:bg-[#141414] z-50 overflow-hidden"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-neutral-100 px-4 py-3 dark:border-[#262626]">
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
                Notifications
              </h3>
              {unreadCount > 0 && (
                <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-neutral-100 px-1.5 text-[10px] font-bold text-neutral-800 dark:bg-[#262626] dark:text-neutral-200">
                  {unreadCount}
                </span>
              )}
            </div>
            {notifications.length > 0 && (
              <button
                type="button"
                onClick={handleMarkAllAsRead}
                disabled={unreadCount === 0}
                className="text-xs font-medium text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100 disabled:opacity-40 disabled:hover:text-neutral-600 dark:disabled:hover:text-neutral-400 disabled:cursor-default transition-colors cursor-pointer"
              >
                Mark all as read
              </button>
            )}
          </div>

          {/* Panel Body */}
          <div className="max-h-[min(380px,calc(100vh-10rem))] overflow-y-auto">
            {/* Loading State */}
            {isLoading ? (
              <div className="p-4 space-y-4">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="flex items-start gap-3 animate-pulse">
                    <div className="w-2 h-2 rounded-full bg-neutral-200 dark:bg-[#262626] mt-1.5 shrink-0" />
                    <div className="flex-1 space-y-2">
                      <div className="h-3.5 w-3/4 rounded bg-neutral-200 dark:bg-[#262626]" />
                      <div className="h-2.5 w-1/2 rounded bg-neutral-100 dark:bg-[#1f1f1f]" />
                      <div className="h-2 w-1/4 rounded bg-neutral-100 dark:bg-[#1f1f1f]" />
                    </div>
                  </div>
                ))}
              </div>
            ) : error ? (
              /* Error State */
              <div className="p-8 text-center flex flex-col items-center justify-center">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400 mb-3">
                  <AlertCircle className="h-5 w-5" />
                </div>
                <p className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
                  Unable to load notifications.
                </p>
                <button
                  type="button"
                  onClick={refetch}
                  className="mt-4 inline-flex items-center gap-1.5 rounded-lg border border-neutral-200 bg-white px-3 py-1.5 text-xs font-medium text-neutral-800 hover:bg-neutral-50 dark:border-[#262626] dark:bg-[#1f1f1f] dark:text-neutral-200 dark:hover:bg-[#262626] transition-colors cursor-pointer"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Try again
                </button>
              </div>
            ) : notifications.length === 0 ? (
              /* Empty State */
              <div className="p-8 text-center flex flex-col items-center justify-center">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-neutral-100 dark:bg-[#1f1f1f] text-neutral-400 dark:text-neutral-500 mb-3">
                  <Check className="h-5 w-5" />
                </div>
                <p className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
                  No notifications
                </p>
                <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                  You’re all caught up.
                </p>
              </div>
            ) : (
              /* Notification Items */
              <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {notifications.map((item) => (
                  <div
                    key={item.id}
                    className={`group relative flex items-start transition-colors ${
                      !item.read
                        ? 'bg-neutral-50/60 dark:bg-[#181818]/60 hover:bg-neutral-100/70 dark:hover:bg-[#1f1f1f]'
                        : 'bg-transparent hover:bg-neutral-50 dark:hover:bg-[#1a1a1a]'
                    }`}
                  >
                    <button
                      type="button"
                      onClick={() => handleNotificationClick(item)}
                      className="flex-1 text-left p-3.5 flex items-start gap-3 cursor-pointer"
                    >
                      {/* Read/Unread Indicator Dot */}
                      <div className="pt-1 shrink-0">
                        {!item.read ? (
                          <span
                            className="block h-2 w-2 rounded-full bg-neutral-900 dark:bg-white"
                            aria-label="Unread notification"
                          />
                        ) : (
                          <span className="block h-2 w-2 rounded-full bg-transparent" />
                        )}
                      </div>

                      {/* Content */}
                      <div className="flex-1 min-w-0 pr-2">
                        <p
                          className={`text-xs leading-snug break-words ${
                            !item.read
                              ? 'font-semibold text-neutral-900 dark:text-neutral-100'
                              : 'font-normal text-neutral-700 dark:text-neutral-300'
                          }`}
                        >
                          {item.title}
                        </p>
                        {item.description && (
                          <p className="text-[11px] text-neutral-500 dark:text-neutral-400 mt-0.5 line-clamp-2 leading-relaxed">
                            {item.description}
                          </p>
                        )}
                        <p className="text-[10px] text-neutral-400 dark:text-neutral-500 mt-1">
                          {item.timestamp}
                        </p>
                      </div>
                    </button>

                    {/* Subtle Dismiss Action on Hover */}
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        dismissNotification(item.id);
                      }}
                      className="opacity-0 group-hover:opacity-100 mt-3 mr-3 p-1 rounded text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200 transition-opacity cursor-pointer"
                      aria-label={`Dismiss ${item.title}`}
                      title="Dismiss"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
