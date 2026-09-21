import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  BarChart3,
  Building2,
  Calendar,
  CheckSquare,
  ChevronLeft,
  ChevronRight,
  Clock,
  CreditCard,
  FileText,
  FolderKanban,
  LayoutDashboard,
  Shield,
  Sliders,
  UserCheck,
  UserPlus,
  Users,
  X,
  FileStack,
  Lock,
  MessageSquare,
  CalendarDays,
  Activity,
  Search,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useEntitlements } from '../../context/EntitlementsContext';
import type { Feature } from '../../api/types';
import { Badge } from '../common/Badge';
import { NexaMark } from '../common/NexaLogo';

export interface SidebarProps {
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  isMobileOpen: boolean;
  onCloseMobile: () => void;
}

interface NavItem {
  label: string;
  path: string;
  icon: React.ElementType;
  badge?: string;
  requiredRole?: 'SUPER_ADMIN' | 'ADMIN' | 'MANAGER' | 'USER';
  requiredFeature?: Feature;
  tierRequired?: 'STARTER' | 'PRO' | 'ENTERPRISE';
}

interface NavGroup {
  groupTitle: string;
  items: NavItem[];
}

export const Sidebar: React.FC<SidebarProps> = ({
  isCollapsed,
  onToggleCollapse,
  isMobileOpen,
  onCloseMobile,
}) => {
  const { user, hasRole } = useAuth();
  const { hasFeature, plan } = useEntitlements();
  const location = useLocation();

  const navGroups: NavGroup[] = [
    {
      groupTitle: 'Overview',
      items: [
        { label: 'Dashboard', path: '/app/dashboard', icon: LayoutDashboard },
        { label: 'Self-Service', path: '/app/self-service', icon: UserCheck },
        { label: 'Projects', path: '/app/projects', icon: FolderKanban, requiredFeature: 'PROJECT_MANAGEMENT', tierRequired: 'STARTER' },
        { label: 'Expense & Claims', path: '/app/claims', icon: FileStack, requiredFeature: 'CLAIMS', tierRequired: 'STARTER' },
        { label: 'Tasks Board', path: '/app/tasks', icon: CheckSquare, requiredFeature: 'TASK_MANAGEMENT', tierRequired: 'STARTER' },
        { label: 'Schedule Planner', path: '/app/schedule', icon: Calendar, requiredFeature: 'WORK_SCHEDULES', tierRequired: 'STARTER' },
        { label: 'Reports & Analytics', path: '/app/reports', icon: BarChart3, requiredFeature: 'BASIC_REPORTS', tierRequired: 'STARTER' },
        { label: 'Global Search', path: '/app/search', icon: Search },
      ],
    },
    {
      groupTitle: 'Workforce & HR',
      items: [
        { label: 'HR Overview', path: '/app/hrm', icon: Users, requiredFeature: 'EMPLOYEE_MANAGEMENT', tierRequired: 'STARTER' },
        { label: 'Employee Directory', path: '/app/hrm/employees', icon: Users, requiredFeature: 'EMPLOYEE_MANAGEMENT', tierRequired: 'STARTER' },
        { label: 'Leave Management', path: '/app/leave', icon: Calendar, requiredFeature: 'ATTENDANCE', tierRequired: 'STARTER' },
        { label: 'Workload & Capacity', path: '/app/workload', icon: Activity, requiredFeature: 'BASIC_REPORTS', tierRequired: 'STARTER' },
        { label: 'Teams & Departments', path: '/app/hrm/teams', icon: Building2, requiredFeature: 'TEAM_MANAGEMENT', tierRequired: 'STARTER' },
        { label: 'Attendance & Leave', path: '/app/hrm/attendance', icon: UserCheck, requiredFeature: 'ATTENDANCE', tierRequired: 'STARTER' },
        { label: 'Time Tracking', path: '/app/hrm/time-tracking', icon: Clock, requiredFeature: 'TIME_TRACKING', tierRequired: 'STARTER' },
        { label: 'Documents & Files', path: '/app/hrm/documents', icon: FileText, requiredFeature: 'DOCUMENTS', tierRequired: 'STARTER' },
      ],
    },
    {
      groupTitle: 'Collaboration',
      items: [
        { label: 'Messages & Chat', path: '/app/chat', icon: MessageSquare, requiredFeature: 'TEAM_CHAT', tierRequired: 'STARTER' },
        { label: 'Calendar', path: '/app/calendar', icon: CalendarDays, requiredFeature: 'CALENDAR', tierRequired: 'STARTER' },
      ],
    },
    {
      groupTitle: 'Organization & Billing',
      items: [
        { label: 'Subscription & Billing', path: '/app/settings/subscription', icon: CreditCard, requiredRole: 'ADMIN' },
        { label: 'User Management', path: '/app/settings/users', icon: UserPlus, requiredRole: 'ADMIN' },
        { label: 'Roles & Permissions', path: '/app/settings/roles', icon: Shield, requiredRole: 'ADMIN' },
        { label: 'Company Settings', path: '/app/settings/company', icon: Building2 },
        { label: 'System Settings', path: '/app/settings/system', icon: Sliders, requiredRole: 'SUPER_ADMIN' },
      ],
    },
  ];

  const content = (
    <div className="flex h-full flex-col justify-between bg-white dark:bg-[#0a0a0a] border-r border-neutral-200 dark:border-[#262626] transition-all duration-300 select-none">
      {/* Brand Header */}
      <div>
        <div
          className={`flex h-16 items-center border-b border-neutral-200 dark:border-[#262626] ${
            isCollapsed ? 'justify-center px-0' : 'justify-between px-4'
          }`}
        >
          <NavLink
            to="/app/dashboard"
            className={`flex items-center overflow-hidden ${isCollapsed ? 'justify-center' : 'gap-3'}`}
            title="Nexa"
          >
            {/* Logo is fixed size and strictly centered in compact mode so it is NEVER clipped */}
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-neutral-100 dark:bg-[#181818] border border-neutral-200/80 dark:border-[#262626] shadow-2xs">
              <NexaMark size={22} />
            </div>
            {!isCollapsed && (
              <div className="flex flex-col min-w-0">
                <span className="text-sm font-bold tracking-tight text-neutral-900 dark:text-neutral-100 truncate">
                  Nexa
                </span>
                <span className="text-[9.5px] text-neutral-400 font-semibold tracking-[0.14em] uppercase truncate">
                  WORKFORCE &amp; OPERATIONS
                </span>
              </div>
            )}
          </NavLink>

          {/* Desktop collapse button only visible in expanded header */}
          {!isCollapsed && (
            <button
              type="button"
              onClick={onToggleCollapse}
              className="hidden lg:flex h-7 w-7 items-center justify-center rounded-lg text-neutral-400 hover:bg-neutral-100 hover:text-neutral-700 dark:hover:bg-[#1a1a1a] dark:hover:text-neutral-200 transition-colors cursor-pointer"
              title="Collapse sidebar"
              aria-label="Collapse sidebar"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
          )}

          {/* Mobile close button */}
          <button
            type="button"
            onClick={onCloseMobile}
            className="flex lg:hidden h-8 w-8 items-center justify-center rounded-lg text-neutral-400 hover:bg-neutral-100 dark:hover:bg-[#1a1a1a] cursor-pointer"
            aria-label="Close menu"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tenant badge in expanded mode */}
        {!isCollapsed && user && (
          <div className="px-4 py-2 bg-neutral-50 dark:bg-[#141414] border-b border-neutral-100 dark:border-[#262626] flex items-center justify-between">
            <span className="text-[11px] font-medium text-neutral-500 dark:text-neutral-400 truncate max-w-[120px]">
              Tenant: <strong className="text-neutral-800 dark:text-neutral-200 font-mono">{user.tenantId}</strong>
            </span>
            <div className="flex items-center gap-1.5">
              <Badge variant={plan === 'ENTERPRISE' || plan === 'PRO' ? 'primary' : 'default'} size="sm">
                {plan}
              </Badge>
              <Badge variant="default" size="sm">
                {user.role}
              </Badge>
            </div>
          </div>
        )}

        {/* Navigation list */}
        <nav
          className={`space-y-6 overflow-y-auto max-h-[calc(100vh-170px)] scrollbar-thin ${
            isCollapsed ? 'p-2' : 'p-3'
          }`}
        >
          {navGroups.map((group) => {
            const filteredItems = group.items.filter((item) =>
              item.requiredRole ? hasRole(item.requiredRole) : true
            );

            if (filteredItems.length === 0) return null;

            return (
              <div key={group.groupTitle} className="space-y-1">
                {!isCollapsed && (
                  <h4 className="px-3 text-[10px] font-bold uppercase tracking-wider text-neutral-400 dark:text-neutral-500 mb-2">
                    {group.groupTitle}
                  </h4>
                )}
                {filteredItems.map((item) => {
                  const Icon = item.icon;
                  const isEntitled = item.requiredFeature ? hasFeature(item.requiredFeature) : true;
                  const isActive =
                    location.pathname === item.path ||
                    (item.path !== '/app/dashboard' && location.pathname.startsWith(item.path));

                  return (
                    <NavLink
                      key={item.path}
                      to={item.path}
                      onClick={onCloseMobile}
                      title={isCollapsed ? (isEntitled ? item.label : `${item.label} (Locked - ${item.tierRequired || 'Upgrade'})`) : undefined}
                      className={`group flex items-center rounded-lg text-xs transition-all ${
                        isCollapsed
                          ? 'h-10 w-10 mx-auto justify-center'
                          : 'gap-3 px-3 py-2 font-medium'
                      } ${
                        isActive
                          ? 'bg-neutral-100 text-neutral-950 dark:bg-[#1a1a1a] dark:text-white font-semibold'
                          : 'text-neutral-600 hover:bg-neutral-100/70 hover:text-neutral-950 dark:text-neutral-400 dark:hover:bg-[#141414] dark:hover:text-white'
                      }`}
                    >
                      <Icon
                        className={`shrink-0 transition-colors ${
                          isCollapsed ? 'w-5 h-5' : 'w-4 h-4'
                        } ${
                          isActive
                            ? 'text-neutral-950 dark:text-white'
                            : 'text-neutral-400 group-hover:text-neutral-700 dark:text-neutral-500 dark:group-hover:text-neutral-200'
                        }`}
                      />
                      {!isCollapsed && (
                        <span className="truncate flex-1">{item.label}</span>
                      )}
                      {!isCollapsed && !isEntitled && (
                        <span className="flex items-center gap-1 text-[10px] text-amber-600 dark:text-amber-400 font-semibold shrink-0">
                          <Lock className="w-3 h-3" />
                          <span>{item.tierRequired || 'PRO'}</span>
                        </span>
                      )}
                      {!isCollapsed && isEntitled && item.badge && (
                        <span className="rounded bg-neutral-200 px-1.5 py-0.5 text-[10px] font-bold text-neutral-800 dark:bg-[#262626] dark:text-neutral-200">
                          {item.badge}
                        </span>
                      )}
                    </NavLink>
                  );
                })}
              </div>
            );
          })}
        </nav>
      </div>

      {/* Footer Toggle / Actions */}
      <div className="p-2 border-t border-neutral-200 dark:border-[#262626] flex flex-col gap-1">
        {/* Toggle Collapse button accessible in both states */}
        <button
          type="button"
          onClick={onToggleCollapse}
          className={`hidden lg:flex items-center rounded-lg text-neutral-500 hover:bg-neutral-100 hover:text-neutral-900 dark:text-neutral-400 dark:hover:bg-[#1a1a1a] dark:hover:text-white transition-colors cursor-pointer ${
            isCollapsed ? 'h-9 w-9 mx-auto justify-center' : 'w-full px-3 py-2 justify-between text-xs font-medium'
          }`}
          title={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {!isCollapsed && <span>Collapse Sidebar</span>}
          {isCollapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>

        {!isCollapsed && (
          <div className="px-3 py-1 text-[11px] text-neutral-400 dark:text-neutral-500 text-center font-mono">
            v1.0 • Enterprise
          </div>
        )}
      </div>
    </div>
  );

  return (
    <>
      {/* Mobile drawer backdrop */}
      {isMobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-xs lg:hidden transition-opacity"
          onClick={onCloseMobile}
        />
      )}

      {/* Desktop sidebar */}
      <aside
        className={`hidden lg:block fixed top-0 left-0 bottom-0 z-30 transition-all duration-300 ${
          isCollapsed ? 'w-16' : 'w-64'
        }`}
      >
        {content}
      </aside>

      {/* Mobile drawer */}
      <aside
        className={`lg:hidden fixed top-0 left-0 bottom-0 z-50 w-72 transition-transform duration-300 ease-in-out ${
          isMobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {content}
      </aside>
    </>
  );
};
