import React from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { AppShell } from '../components/layout/AppShell';
import { LoginPage } from '../pages/auth/LoginPage';
import { SignupPage } from '../pages/auth/SignupPage';
import { AcceptInvitePage } from '../pages/auth/AcceptInvitePage';
import { DashboardPage } from '../pages/dashboard/DashboardPage';
import { ProjectsPage } from '../pages/projects/ProjectsPage';
import { ClaimsPage } from '../pages/claims/ClaimsPage';
import { TasksPage } from '../pages/tasks/TasksPage';
import { SchedulePage } from '../pages/schedule/SchedulePage';
import { TimeTrackingPage } from '../pages/hrm/TimeTrackingPage';
import { ReportsPage } from '../pages/reports/ReportsPage';
import { HrmOverviewPage } from '../pages/hrm/HrmOverviewPage';
import { EmployeeListPage } from '../pages/hrm/EmployeeListPage';
import { EmployeeProfilePage } from '../pages/hrm/EmployeeProfilePage';
import { TeamsPage } from '../pages/hrm/TeamsPage';
import { AttendanceLeavePage } from '../pages/hrm/AttendanceLeavePage';
import { WorkSchedulesPage } from '../pages/hrm/WorkSchedulesPage';
import { DocumentsPage } from '../pages/hrm/DocumentsPage';
import { BillingPage } from '../pages/billing/BillingPage';
import { BillingSuccessPage } from '../pages/billing/BillingSuccessPage';
import { BillingCancelPage } from '../pages/billing/BillingCancelPage';
import { SettingsLayout } from '../pages/settings/SettingsLayout';
import { SubscriptionSettingsPage } from '../pages/settings/SubscriptionSettingsPage';
import { CompanySettingsPage } from '../pages/settings/CompanySettingsPage';
import { UserManagementPage } from '../pages/settings/UserManagementPage';
import { RolesPermissionsPage } from '../pages/settings/RolesPermissionsPage';
import { WorkScheduleModelsPage } from '../pages/settings/WorkScheduleModelsPage';
import { AbsenceTypesPage } from '../pages/settings/AbsenceTypesPage';
import { IntegrationsPage } from '../pages/settings/IntegrationsPage';
import { SystemSettingsPage } from '../pages/settings/SystemSettingsPage';
import { LandingPage } from '../pages/LandingPage';
import type { Role } from '../api/types';
import { ShieldAlert } from 'lucide-react';
import { NexaMark } from '../components/common/NexaLogo';

const ProtectedRoute: React.FC<{
  children: React.ReactNode;
  requiredRole?: Role;
}> = ({ children, requiredRole }) => {
  const { isAuthenticated, isLoading, hasRole } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-neutral-50 dark:bg-[#0a0a0a]">
        <div className="flex flex-col items-center gap-4">
          <div className="relative flex items-center justify-center">
            <NexaMark size={32} />
          </div>
          <div className="flex items-center gap-2">
            <div className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-neutral-900 dark:border-white border-t-transparent" />
            <span className="text-xs text-neutral-500 dark:text-neutral-400 font-medium">Loading session...</span>
          </div>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredRole && !hasRole(requiredRole)) {
    return (
      <div className="p-8 max-w-lg mx-auto text-center space-y-4">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-red-100 text-red-600 dark:bg-red-950/60 dark:text-red-400 mx-auto">
          <ShieldAlert className="w-6 h-6" />
        </div>
        <h2 className="text-lg font-bold text-neutral-900 dark:text-neutral-100">
          Access Restricted
        </h2>
        <p className="text-xs text-neutral-500 dark:text-neutral-400 leading-relaxed">
          Your active workspace role is not authorized to view this page. Contact your organization administrator if you believe this is an error.
        </p>
        <Navigate to="/app/dashboard" />
      </div>
    );
  }

  return <>{children}</>;
};

export const AppRoutes: React.FC = () => {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      {/* Public Auth Routes */}
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/app/dashboard" replace /> : <LoginPage />}
      />
      <Route
        path="/signup"
        element={isAuthenticated ? <Navigate to="/app/dashboard" replace /> : <SignupPage />}
      />
      <Route path="/accept-invite" element={<AcceptInvitePage />} />

      {/* Stripe Redirect Handlers */}
      <Route path="/billing/success" element={<BillingSuccessPage />} />
      <Route path="/billing/cancel" element={<BillingCancelPage />} />
      <Route path="/billing" element={<Navigate to="/app/billing" replace />} />

      {/* Authenticated Application Shell */}
      <Route
        path="/app"
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/app/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="projects" element={<ProjectsPage />} />
        <Route path="claims" element={<ClaimsPage />} />
        <Route path="tasks" element={<TasksPage />} />
        <Route path="schedule" element={<SchedulePage />} />
        <Route path="time-tracking" element={<TimeTrackingPage />} />
        <Route path="reports" element={<ReportsPage />} />

        {/* HRM Module */}
        <Route path="hrm" element={<HrmOverviewPage />} />
        <Route path="hrm/employees" element={<EmployeeListPage />} />
        <Route path="hrm/employees/:id" element={<EmployeeProfilePage />} />
        <Route path="hrm/teams" element={<TeamsPage />} />
        <Route path="hrm/attendance" element={<AttendanceLeavePage />} />
        <Route path="hrm/leave" element={<AttendanceLeavePage />} />
        <Route path="hrm/work-schedules" element={<WorkSchedulesPage />} />
        <Route path="hrm/time-tracking" element={<TimeTrackingPage />} />
        <Route path="hrm/documents" element={<DocumentsPage />} />

        {/* Billing */}
        <Route
          path="billing"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <BillingPage />
            </ProtectedRoute>
          }
        />

        {/* Settings Area */}
        <Route path="settings" element={<SettingsLayout />}>
          <Route index element={<Navigate to="company" replace />} />
          <Route path="company" element={<CompanySettingsPage />} />
          <Route path="subscription" element={<SubscriptionSettingsPage />} />
          <Route
            path="users"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <UserManagementPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="roles"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <RolesPermissionsPage />
              </ProtectedRoute>
            }
          />
          <Route path="work-schedules" element={<WorkScheduleModelsPage />} />
          <Route path="absence-types" element={<AbsenceTypesPage />} />
          <Route path="integrations" element={<IntegrationsPage />} />
          <Route
            path="system"
            element={
              <ProtectedRoute requiredRole="SUPER_ADMIN">
                <SystemSettingsPage />
              </ProtectedRoute>
            }
          />
        </Route>
      </Route>

      {/* Public Landing Page */}
      <Route path="/" element={<LandingPage />} />

      {/* 404 Catch-All */}
      <Route
        path="*"
        element={
          <div className="min-h-screen flex flex-col items-center justify-center p-4 text-center bg-neutral-50 dark:bg-[#0a0a0a]">
            <NexaMark size={40} className="mb-4" />
            <h1 className="text-4xl font-bold text-neutral-900 dark:text-neutral-100 mb-2">404</h1>
            <p className="text-sm text-neutral-500 mb-6">Page not found in this workspace.</p>
            <Navigate to="/app/dashboard" />
          </div>
        }
      />
    </Routes>
  );
};
