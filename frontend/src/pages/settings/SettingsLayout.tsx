import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import {
  Building2,
  Calendar,
  Layers,
  Shield,
  Sliders,
  UserCheck,
  UserPlus,
} from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { useAuth } from '../../context/AuthContext';

export const SettingsLayout: React.FC = () => {
  const { hasRole } = useAuth();

  const settingsNav = [
    { label: 'Company Profile', path: '/app/settings/company', icon: Building2 },
    { label: 'User Management', path: '/app/settings/users', icon: UserPlus, requiredRole: 'ADMIN' as const },
    { label: 'Roles & Permissions', path: '/app/settings/roles', icon: Shield, requiredRole: 'ADMIN' as const },
    { label: 'Work Schedules', path: '/app/settings/work-schedules', icon: Calendar },
    { label: 'Absence Policies', path: '/app/settings/absence-types', icon: UserCheck },
    { label: 'Integrations', path: '/app/settings/integrations', icon: Layers },
    { label: 'System Settings', path: '/app/settings/system', icon: Sliders, requiredRole: 'SUPER_ADMIN' as const },
  ];

  const filteredNav = settingsNav.filter((item) =>
    item.requiredRole ? hasRole(item.requiredRole) : true
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title="Workspace Configuration & Settings"
        description="Manage organizational identity, teammate invitation access, RBAC policies, and integrations."
      />

      <div className="flex flex-col lg:flex-row gap-6">
        {/* Settings Secondary Nav */}
        <aside className="w-full lg:w-60 shrink-0">
          <nav className="flex lg:flex-col gap-1 overflow-x-auto p-1 bg-white dark:bg-[#141414] rounded-xl border border-neutral-200 dark:border-[#262626]">
            {filteredNav.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }) =>
                    `flex items-center gap-2.5 px-3.5 py-2.5 rounded-lg text-xs font-medium whitespace-nowrap transition-colors ${
                      isActive
                        ? 'bg-neutral-100 text-neutral-950 font-semibold dark:bg-[#1f1f1f] dark:text-white'
                        : 'text-neutral-600 hover:bg-neutral-50 hover:text-neutral-950 dark:text-neutral-400 dark:hover:bg-[#1f1f1f] dark:hover:text-neutral-100'
                    }`
                  }
                >
                  <Icon className="w-4 h-4 shrink-0" />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>
        </aside>

        {/* Settings Viewport */}
        <div className="flex-1 min-w-0">
          <Outlet />
        </div>
      </div>
    </div>
  );
};
