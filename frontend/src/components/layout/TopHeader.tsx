import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  CreditCard,
  LogOut,
  Menu,
  Moon,
  Search,
  Settings,
  Sun,
  User,
  UserPlus,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { Button } from '../common/Button';
import { Dropdown } from '../common/Dropdown';
import { NotificationDropdown } from './NotificationDropdown';

export interface TopHeaderProps {
  onToggleMobileSidebar: () => void;
  onOpenInviteModal?: () => void;
}

export const TopHeader: React.FC<TopHeaderProps> = ({
  onToggleMobileSidebar,
  onOpenInviteModal,
}) => {
  const { user, logout, hasRole } = useAuth();
  const { resolvedTheme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/app/hrm/employees?search=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  const userInitial = user?.email ? user.email.charAt(0).toUpperCase() : 'U';

  const userDropdownItems = [
    {
      id: 'profile',
      label: user?.email || 'User Profile',
      icon: <User className="w-4 h-4" />,
      onClick: () => navigate('/app/settings/company'),
    },
    {
      id: 'billing',
      label: 'Billing & Invoices',
      icon: <CreditCard className="w-4 h-4" />,
      onClick: () => navigate('/app/billing'),
      disabled: !hasRole('ADMIN'),
    },
    {
      id: 'settings',
      label: 'Settings',
      icon: <Settings className="w-4 h-4" />,
      onClick: () => navigate('/app/settings/company'),
    },
    'divider' as const,
    {
      id: 'logout',
      label: 'Log out',
      icon: <LogOut className="w-4 h-4" />,
      danger: true,
      onClick: () => {
        logout();
        navigate('/login');
      },
    },
  ];

  return (
    <header className="sticky top-0 z-20 flex h-16 w-full items-center justify-between border-b border-neutral-200 bg-white/95 px-4 backdrop-blur-xs dark:border-[#262626] dark:bg-[#0a0a0a]/95 transition-colors select-none">
      <div className="flex items-center gap-3 flex-1 max-w-xl">
        {/* Mobile menu toggle */}
        <button
          type="button"
          onClick={onToggleMobileSidebar}
          className="lg:hidden p-2 rounded-lg text-neutral-500 hover:bg-neutral-100 hover:text-neutral-700 dark:text-neutral-400 dark:hover:bg-[#1a1a1a] cursor-pointer"
          aria-label="Open sidebar"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Global Search */}
        <form onSubmit={handleSearchSubmit} className="relative w-full max-w-md hidden sm:block">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
          <input
            type="text"
            placeholder="Search employees, tasks, projects..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-lg border border-neutral-200 bg-neutral-50 pl-9 pr-4 py-1.5 text-xs text-neutral-900 placeholder-neutral-400 transition-colors focus:bg-white focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100 dark:placeholder-neutral-500 dark:focus:bg-[#0a0a0a] dark:focus:border-white"
          />
        </form>
      </div>

      {/* Right side controls */}
      <div className="flex items-center gap-2 sm:gap-3">
        {/* Quick invite action for Admins */}
        {hasRole('ADMIN') && (
          <Button
            size="sm"
            variant="outline"
            leftIcon={<UserPlus className="w-3.5 h-3.5" />}
            onClick={() => {
              if (onOpenInviteModal) {
                onOpenInviteModal();
              } else {
                navigate('/app/settings/users');
              }
            }}
            className="hidden sm:inline-flex text-xs font-semibold"
          >
            Invite Teammate
          </Button>
        )}

        {/* Theme toggle */}
        <button
          type="button"
          onClick={toggleTheme}
          className="p-2 rounded-lg text-neutral-500 hover:bg-neutral-100 hover:text-neutral-800 dark:text-neutral-400 dark:hover:bg-[#1a1a1a] dark:hover:text-neutral-100 transition-colors cursor-pointer"
          aria-label={`Switch to ${resolvedTheme === 'dark' ? 'light' : 'dark'} mode`}
          title={`Switch to ${resolvedTheme === 'dark' ? 'light' : 'dark'} mode`}
        >
          {resolvedTheme === 'dark' ? (
            <Sun className="w-4 h-4 text-neutral-100" />
          ) : (
            <Moon className="w-4 h-4 text-neutral-800" />
          )}
        </button>

        {/* Notifications */}
        <NotificationDropdown />

        <div className="h-5 w-px bg-neutral-200 dark:bg-[#262626] mx-1 hidden sm:block" />

        {/* User profile dropdown */}
        <Dropdown
          align="right"
          trigger={
            <button
              type="button"
              className="flex items-center gap-2 rounded-lg p-1.5 hover:bg-neutral-100 dark:hover:bg-[#1a1a1a] transition-colors cursor-pointer"
              aria-label="User menu"
            >
              <div className="flex h-7 w-7 items-center justify-center rounded-full bg-neutral-900 text-white dark:bg-white dark:text-[#0a0a0a] text-xs font-bold">
                {userInitial}
              </div>
              <div className="hidden md:flex flex-col text-left">
                <span className="text-xs font-semibold text-neutral-900 dark:text-neutral-100 leading-none">
                  {user?.email ? user.email.split('@')[0] : 'Admin'}
                </span>
                <span className="text-[10px] text-neutral-400 leading-none mt-1">
                  {user?.role || 'MEMBER'}
                </span>
              </div>
            </button>
          }
          items={userDropdownItems}
        />
      </div>
    </header>
  );
};
