import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Building2,
  Calendar,
  CheckCircle2,
  CheckSquare,
  Clock,
  Edit3,
  FolderKanban,
  Mail,
  MapPin,
  Phone,
  RefreshCw,
  Shield,
  User,
  UserCheck,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { selfServiceApi } from '../../api/selfServiceApi';
import type {
  SelfServiceOverview,
  SelfServiceProfile,
  UpdateSelfServiceProfileRequest,
} from '../../api/types';
import { formatDate } from '../../utils/formatters';

export const SelfServicePage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [overview, setOverview] = useState<SelfServiceOverview | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Edit form state
  const [editForm, setEditForm] = useState<UpdateSelfServiceProfileRequest>({
    name: '',
    phone: '',
    location: '',
    workModel: 'REMOTE',
  });

  const loadData = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await selfServiceApi.getOverview();
      setOverview(data);
      if (data.profile) {
        setEditForm({
          name: data.profile.name || '',
          phone: data.profile.phone || '',
          location: data.profile.location || '',
          workModel: data.profile.workModel || 'REMOTE',
        });
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load self-service profile';
      showToast('error', 'Error', msg);
    } finally {
      setIsLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    let isMounted = true;
    selfServiceApi.getOverview()
      .then((data) => {
        if (isMounted) {
          setOverview(data);
          if (data.profile) {
            setEditForm({
              name: data.profile.name || '',
              phone: data.profile.phone || '',
              location: data.profile.location || '',
              workModel: data.profile.workModel || 'REMOTE',
            });
          }
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg = err instanceof Error ? err.message : 'Failed to load self-service profile';
          showToast('error', 'Error', msg);
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [showToast]);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setIsSaving(true);
      const updatedProfile: SelfServiceProfile = await selfServiceApi.updateProfile(editForm);
      setOverview((prev) => (prev ? { ...prev, profile: updatedProfile } : null));
      showToast('success', 'Profile Updated', 'Your personal self-service profile has been updated.');
      setIsEditModalOpen(false);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update profile';
      showToast('error', 'Update Failed', msg);
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title="Employee Self-Service"
          description="Manage your profile, view assigned projects, tasks, and leave balances."
        />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <LoadingSkeleton className="h-64 rounded-xl" />
          <LoadingSkeleton className="h-64 rounded-xl md:col-span-2" />
        </div>
        <LoadingSkeleton className="h-96 rounded-xl" />
      </div>
    );
  }

  const profile = overview?.profile;
  const leaveBalances = overview?.leaveBalances || [];
  const assignedTasks = overview?.assignedTasks || [];
  const assignedProjects = overview?.assignedProjects || [];
  const upcomingEvents = overview?.upcomingEvents || [];

  const initial = profile?.name ? profile.name.charAt(0).toUpperCase() : (user?.email?.charAt(0).toUpperCase() || 'U');

  return (
    <div className="space-y-6">
      <PageHeader
        title="Employee Self-Service"
        description="Review personal operational profile, balances, active task allocations, and calendar schedule."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={loadData}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              Sync
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => setIsEditModalOpen(true)}
              leftIcon={<Edit3 className="w-3.5 h-3.5" />}
            >
              Edit Profile
            </Button>
          </div>
        }
      />

      {/* Top Section: Profile Header & Summary */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Profile Card */}
        <Card className="lg:col-span-1 flex flex-col justify-between">
          <CardHeader>
            <CardTitle className="text-sm font-bold flex items-center gap-2">
              <User className="w-4 h-4 text-neutral-900 dark:text-neutral-100" />
              <span>Personal Identity</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
            <div className="flex items-center gap-4">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-neutral-900 text-white dark:bg-white dark:text-neutral-950 text-2xl font-bold shadow-md">
                {initial}
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="text-base font-bold text-neutral-900 dark:text-neutral-100 truncate">
                  {profile?.name || user?.email?.split('@')[0] || 'Employee'}
                </h3>
                <p className="text-xs text-neutral-500 dark:text-neutral-400 truncate">
                  {profile?.position || 'Staff Member'}
                </p>
                <div className="flex items-center gap-1.5 mt-1.5 flex-wrap">
                  <Badge variant="primary" size="sm">
                    {profile?.role || user?.role || 'USER'}
                  </Badge>
                  <Badge variant={profile?.status === 'ACTIVE' ? 'success' : 'default'} size="sm">
                    {profile?.status || 'ACTIVE'}
                  </Badge>
                </div>
              </div>
            </div>

            <div className="space-y-2.5 pt-3 border-t border-neutral-100 dark:border-[#262626] text-xs">
              <div className="flex items-center gap-2.5 text-neutral-600 dark:text-neutral-400">
                <Mail className="w-4 h-4 text-neutral-400 shrink-0" />
                <span className="truncate">{profile?.email || user?.email}</span>
              </div>
              <div className="flex items-center gap-2.5 text-neutral-600 dark:text-neutral-400">
                <Building2 className="w-4 h-4 text-neutral-400 shrink-0" />
                <span>Department: <strong className="text-neutral-900 dark:text-neutral-100">{profile?.department || 'General'}</strong></span>
              </div>
              <div className="flex items-center gap-2.5 text-neutral-600 dark:text-neutral-400">
                <Phone className="w-4 h-4 text-neutral-400 shrink-0" />
                <span>{profile?.phone || 'No phone recorded'}</span>
              </div>
              <div className="flex items-center gap-2.5 text-neutral-600 dark:text-neutral-400">
                <MapPin className="w-4 h-4 text-neutral-400 shrink-0" />
                <span>{profile?.location || 'Remote / Unspecified'} ({profile?.workModel || 'REMOTE'})</span>
              </div>
              <div className="flex items-center gap-2.5 text-neutral-600 dark:text-neutral-400">
                <Shield className="w-4 h-4 text-neutral-400 shrink-0" />
                <span>Tenant: <code className="font-mono text-[11px] text-neutral-800 dark:text-neutral-200">{profile?.tenantId || user?.tenantId}</code></span>
              </div>
              {profile?.manager && (
                <div className="flex items-center gap-2.5 text-neutral-600 dark:text-neutral-400">
                  <UserCheck className="w-4 h-4 text-neutral-400 shrink-0" />
                  <span>Reporting Manager: <strong className="text-neutral-900 dark:text-neutral-100">{profile.manager}</strong></span>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Leave Balances Grid */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-neutral-900 dark:text-neutral-100">Leave Accruals &amp; Balances</h3>
              <p className="text-xs text-neutral-500 dark:text-neutral-400">Available annual, sick, and personal allowance for {new Date().getFullYear()}</p>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate('/app/leave')}
              leftIcon={<Calendar className="w-3.5 h-3.5" />}
            >
              Request Leave
            </Button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            {leaveBalances.map((bal) => (
              <Card key={bal.id} className="p-4 flex flex-col justify-between">
                <div>
                  <span className="text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:text-neutral-500">
                    {bal.leaveType.replace('_', ' ')}
                  </span>
                  <div className="flex items-baseline gap-1 mt-2">
                    <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                      {bal.remainingDays}
                    </span>
                    <span className="text-xs text-neutral-500">days left</span>
                  </div>
                </div>
                <div className="mt-4 pt-3 border-t border-neutral-100 dark:border-[#262626] text-[11px] text-neutral-500 space-y-1">
                  <div className="flex justify-between">
                    <span>Allocated:</span>
                    <span className="font-semibold text-neutral-800 dark:text-neutral-200">{bal.totalDays}d</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Used:</span>
                    <span className="font-semibold text-neutral-800 dark:text-neutral-200">{bal.usedDays}d</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Pending:</span>
                    <span className="font-semibold text-amber-600 dark:text-amber-400">{bal.pendingDays}d</span>
                  </div>
                </div>
              </Card>
            ))}
          </div>

          {/* KPI Mini-Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
            <Card className="p-4">
              <span className="text-xs font-semibold text-neutral-500 dark:text-neutral-400">Assigned Open Tasks</span>
              <div className="flex items-baseline gap-2 mt-1">
                <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                  {assignedTasks.filter((t) => t.status !== 'DONE').length}
                </span>
                <span className="text-xs text-neutral-400">of {assignedTasks.length} total</span>
              </div>
            </Card>
            <Card className="p-4">
              <span className="text-xs font-semibold text-neutral-500 dark:text-neutral-400">Assigned Projects</span>
              <div className="flex items-baseline gap-2 mt-1">
                <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                  {assignedProjects.length}
                </span>
                <span className="text-xs text-neutral-400">active engagements</span>
              </div>
            </Card>
            <Card className="p-4">
              <span className="text-xs font-semibold text-neutral-500 dark:text-neutral-400">Upcoming Events</span>
              <div className="flex items-baseline gap-2 mt-1">
                <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                  {upcomingEvents.length}
                </span>
                <span className="text-xs text-neutral-400">on your schedule</span>
              </div>
            </Card>
          </div>
        </div>
      </div>

      {/* Main Section: Tasks & Projects */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Assigned Tasks */}
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="text-sm font-bold flex items-center gap-2">
              <CheckSquare className="w-4 h-4 text-neutral-900 dark:text-neutral-100" />
              <span>My Assigned Tasks</span>
            </CardTitle>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => navigate('/app/tasks')}
            >
              Go to Tasks Board
            </Button>
          </CardHeader>
          <CardContent className="p-0">
            {assignedTasks.length === 0 ? (
              <div className="p-8 text-center text-xs text-neutral-500">
                No tasks currently assigned to you. Enjoy the clear schedule!
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Task</th>
                      <th className="px-5 py-3">Status</th>
                      <th className="px-5 py-3">Priority</th>
                      <th className="px-5 py-3">Due Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {assignedTasks.map((t) => (
                      <tr key={t.id} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors">
                        <td className="px-5 py-3 font-semibold text-neutral-900 dark:text-neutral-100 max-w-xs truncate">
                          {t.title}
                        </td>
                        <td className="px-5 py-3">
                          <Badge
                            variant={
                              t.status === 'DONE'
                                ? 'success'
                                : t.status === 'IN_PROGRESS'
                                ? 'primary'
                                : 'default'
                            }
                            size="sm"
                          >
                            {t.status}
                          </Badge>
                        </td>
                        <td className="px-5 py-3">
                          <Badge
                            variant={
                              t.priority === 'URGENT' || t.priority === 'HIGH'
                                ? 'danger'
                                : t.priority === 'MEDIUM'
                                ? 'warning'
                                : 'default'
                            }
                            size="sm"
                          >
                            {t.priority}
                          </Badge>
                        </td>
                        <td className="px-5 py-3 whitespace-nowrap">
                          {t.dueDate ? formatDate(t.dueDate) : 'No due date'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Assigned Projects & Schedule */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-bold flex items-center gap-2">
                <FolderKanban className="w-4 h-4 text-neutral-900 dark:text-neutral-100" />
                <span>My Projects</span>
              </CardTitle>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate('/app/projects')}
              >
                View All
              </Button>
            </CardHeader>
            <CardContent className="p-0 divide-y divide-neutral-100 dark:divide-[#262626]">
              {assignedProjects.length === 0 ? (
                <div className="p-6 text-center text-xs text-neutral-500">
                  Not assigned to any active projects.
                </div>
              ) : (
                assignedProjects.map((proj) => (
                  <div
                    key={proj.id}
                    className="p-3.5 hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors cursor-pointer"
                    onClick={() => navigate(`/app/projects/${proj.id}`)}
                  >
                    <div className="flex items-center justify-between">
                      <h4 className="text-xs font-bold text-neutral-900 dark:text-neutral-100 truncate">
                        {proj.name}
                      </h4>
                      <Badge variant="default" size="sm">
                        {proj.status}
                      </Badge>
                    </div>
                    {proj.client && (
                      <p className="text-[11px] text-neutral-400 mt-0.5">
                        Client: {proj.client}
                      </p>
                    )}
                  </div>
                ))
              )}
            </CardContent>
          </Card>

          {/* Upcoming Events */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-bold flex items-center gap-2">
                <Clock className="w-4 h-4 text-neutral-900 dark:text-neutral-100" />
                <span>Upcoming Calendar</span>
              </CardTitle>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate('/app/calendar')}
              >
                Open Calendar
              </Button>
            </CardHeader>
            <CardContent className="p-0 divide-y divide-neutral-100 dark:divide-[#262626]">
              {upcomingEvents.length === 0 ? (
                <div className="p-6 text-center text-xs text-neutral-500">
                  No upcoming meetings scheduled.
                </div>
              ) : (
                upcomingEvents.slice(0, 5).map((evt) => (
                  <div key={evt.id} className="p-3.5">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-neutral-900 dark:text-neutral-100 truncate">
                        {evt.title}
                      </span>
                      <span className="text-[10px] text-neutral-400 font-mono">
                        {evt.startTime ? formatDate(evt.startTime) : ''}
                      </span>
                    </div>
                    {evt.description && (
                      <p className="text-[11px] text-neutral-500 truncate mt-0.5">
                        {evt.description}
                      </p>
                    )}
                  </div>
                ))
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Edit Profile Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Edit Personal Information"
        description="Update your display name, contact phone, and working location. Organization roles and emails are governed by workspace administrators."
        footer={
          <>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsEditModalOpen(false)}
              disabled={isSaving}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleUpdateProfile}
              isLoading={isSaving}
              leftIcon={<CheckCircle2 className="w-3.5 h-3.5" />}
            >
              Save Changes
            </Button>
          </>
        }
      >
        <form onSubmit={handleUpdateProfile} className="space-y-4">
          <Input
            label="Full Name"
            value={editForm.name}
            onChange={(e) => setEditForm((prev) => ({ ...prev, name: e.target.value }))}
            placeholder="Jane Doe"
            required
          />
          <Input
            label="Phone Number"
            value={editForm.phone}
            onChange={(e) => setEditForm((prev) => ({ ...prev, phone: e.target.value }))}
            placeholder="+1 (555) 000-0000"
          />
          <Input
            label="Work Location"
            value={editForm.location}
            onChange={(e) => setEditForm((prev) => ({ ...prev, location: e.target.value }))}
            placeholder="San Francisco, CA / London, UK"
          />
          <Select
            label="Work Model"
            value={editForm.workModel}
            onChange={(e) => setEditForm((prev) => ({ ...prev, workModel: e.target.value }))}
            options={[
              { value: 'REMOTE', label: 'Remote' },
              { value: 'HYBRID', label: 'Hybrid' },
              { value: 'ON_SITE', label: 'On-site' },
            ]}
          />

          <div className="p-3 bg-neutral-50 dark:bg-[#1f1f1f] rounded-lg border border-neutral-200 dark:border-[#262626] text-[11px] text-neutral-500">
            <span className="font-semibold text-neutral-700 dark:text-neutral-300">Protected Fields:</span> Workspace role (<code>{profile?.role}</code>), account email (<code>{profile?.email}</code>), and tenant ID cannot be modified through self-service.
          </div>
        </form>
      </Modal>
    </div>
  );
};
