import React, { useCallback, useEffect, useState } from 'react';
import {
  AlertTriangle,
  Check,
  Copy,
  KeyRound,
  MoreVertical,
  Plus,
  RefreshCw,
  Shield,
  UserCheck,
  UserPlus,
  UserX,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { Dropdown, type DropdownItem } from '../../components/common/Dropdown';
import { Modal } from '../../components/common/Modal';
import { Select } from '../../components/common/Select';
import { EmployeeInviteModal } from '../hrm/EmployeeInviteModal';
import { usersApi } from '../../api/usersApi';
import type { Role, WorkspaceUser } from '../../api/types';
import { formatDate } from '../../utils/formatters';

const getAvailableTargetRoles = (currentUserRole: Role | undefined, targetUserRole: Role): Role[] => {
  if (!currentUserRole) return [];
  if (targetUserRole === 'SUPER_ADMIN') return [];

  if (currentUserRole === 'SUPER_ADMIN') {
    if (targetUserRole === 'USER') return ['MANAGER', 'ADMIN'];
    if (targetUserRole === 'MANAGER') return ['USER', 'ADMIN'];
    if (targetUserRole === 'ADMIN') return ['MANAGER', 'USER'];
  } else if (currentUserRole === 'ADMIN') {
    if (targetUserRole === 'USER') return ['MANAGER'];
    if (targetUserRole === 'MANAGER') return ['USER'];
  }
  return [];
};

const canManageUser = (
  currentUserRole: Role | undefined,
  currentUserId: string | undefined,
  targetUser: WorkspaceUser
): boolean => {
  if (!currentUserRole || !currentUserId) return false;
  if (targetUser.id === currentUserId) return false;
  if (targetUser.role === 'SUPER_ADMIN') return false;
  if (targetUser.status === 'DISABLED') return false;

  if (currentUserRole === 'SUPER_ADMIN') return true;
  if (currentUserRole === 'ADMIN') {
    return targetUser.role === 'USER' || targetUser.role === 'MANAGER';
  }
  return false;
};

export const UserManagementPage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
  const [copiedToken, setCopiedToken] = useState<string | null>(null);

  const [users, setUsers] = useState<WorkspaceUser[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // Modals state
  const [roleModalUser, setRoleModalUser] = useState<WorkspaceUser | null>(null);
  const [selectedNewRole, setSelectedNewRole] = useState<Role | ''>('');
  const [deactivateModalUser, setDeactivateModalUser] = useState<WorkspaceUser | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleRefresh = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await usersApi.getUsers();
      setUsers(data);
    } catch {
      // Handled silently or empty state
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;
    usersApi
      .getUsers()
      .then((data) => {
        if (isMounted && data) {
          setUsers(data);
        }
      })
      .catch(() => {});
    return () => {
      isMounted = false;
    };
  }, []);

  const handleInviteSuccess = () => {
    handleRefresh();
  };

  const handleCopyLink = (token: string) => {
    const inviteUrl = `${window.location.origin}/accept-invite?token=${token}&tenantId=${user?.tenantId}`;
    navigator.clipboard.writeText(inviteUrl);
    setCopiedToken(token);
    showToast('success', 'Invite URL Copied', 'Share this activation link with your teammate.');
    setTimeout(() => setCopiedToken(null), 2500);
  };

  const handleOpenChangeRole = (target: WorkspaceUser) => {
    const available = getAvailableTargetRoles(user?.role, target.role);
    setRoleModalUser(target);
    setSelectedNewRole(available[0] || '');
  };

  const handleConfirmChangeRole = async () => {
    if (!roleModalUser || !selectedNewRole) return;
    setIsSubmitting(true);
    try {
      await usersApi.changeRole(roleModalUser.id, selectedNewRole);
      showToast(
        'success',
        'Role Updated',
        `Successfully changed ${roleModalUser.email}'s role to ${selectedNewRole}.`
      );
      setRoleModalUser(null);
      handleRefresh();
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error
          ? err.message
          : 'Failed to update user role. Check permissions and try again.';
      showToast('error', 'Role Change Failed', errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmDeactivate = async () => {
    if (!deactivateModalUser) return;
    setIsSubmitting(true);
    try {
      await usersApi.deactivateUser(deactivateModalUser.id);
      showToast(
        'success',
        'User Deactivated',
        `${deactivateModalUser.email} has been deactivated from this workspace.`
      );
      setDeactivateModalUser(null);
      handleRefresh();
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error
          ? err.message
          : 'Failed to deactivate user. Check permissions and try again.';
      showToast('error', 'Deactivation Failed', errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  const activeUsers = users.filter((u) => u.status === 'ACTIVE');
  const pendingInvites = users.filter((u) => u.status === 'INVITED');
  const deactivatedUsers = users.filter((u) => u.status === 'DISABLED' || u.status === 'SUSPENDED');

  const availableRolesForModal = roleModalUser
    ? getAvailableTargetRoles(user?.role, roleModalUser.role)
    : [];

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <UserPlus className="w-5 h-5 text-zinc-900 dark:text-neutral-100" />
              <span>Workspace User Management & Access Controls</span>
            </CardTitle>
            <p className="text-xs text-zinc-500 dark:text-neutral-400 mt-1">
              Manage workspace teammates, assign RBAC roles, control account activation, and issue onboarding invitation links.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleRefresh}
              isLoading={isLoading}
              leftIcon={<RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />}
            >
              Refresh
            </Button>
            <Button
              size="sm"
              onClick={() => setIsInviteModalOpen(true)}
              leftIcon={<Plus className="w-4 h-4" />}
            >
              Invite New User
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Current user card */}
          <div className="p-4 rounded-xl border border-neutral-200 bg-neutral-50 dark:border-[#262626] dark:bg-[#141414] flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-neutral-950 text-white dark:bg-white dark:text-[#0a0a0a] font-bold text-sm">
                {user?.email ? user.email.charAt(0).toUpperCase() : 'A'}
              </div>
              <div>
                <p className="text-xs font-semibold text-neutral-900 dark:text-neutral-100 flex items-center gap-2">
                  <span>{user?.email}</span>
                  <span className="text-[10px] text-neutral-500 font-normal">
                    (Your Active Session)
                  </span>
                </p>
                <p className="text-[11px] text-neutral-500 font-mono mt-0.5">
                  ID: {user?.id}
                </p>
              </div>
            </div>
            <Badge variant="primary" size="sm">
              {user?.role}
            </Badge>
          </div>

          {/* Active Workspace Members */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-neutral-400 dark:text-neutral-500 mb-3 flex items-center gap-1.5">
              <UserCheck className="w-4 h-4" />
              <span>Active Workspace Members ({activeUsers.length})</span>
            </h4>
            <div className="overflow-x-auto rounded-xl border border-neutral-200 dark:border-[#262626]">
              <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                  <tr>
                    <th className="px-5 py-3">Member Email</th>
                    <th className="px-5 py-3">Assigned Role</th>
                    <th className="px-5 py-3">Joined Date</th>
                    <th className="px-5 py-3">Status</th>
                    <th className="px-5 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                  {activeUsers.map((u) => {
                    const isSelf = u.id === user?.id;
                    const canManage = canManageUser(user?.role, user?.id, u);
                    const availableRoles = getAvailableTargetRoles(user?.role, u.role);

                    const dropdownItems: (DropdownItem | 'divider')[] = [];
                    if (canManage) {
                      if (availableRoles.length > 0) {
                        dropdownItems.push({
                          id: 'change-role',
                          label: 'Change Role',
                          icon: <Shield className="w-3.5 h-3.5 text-neutral-500" />,
                          onClick: () => handleOpenChangeRole(u),
                        });
                      }
                      dropdownItems.push({
                        id: 'deactivate',
                        label: 'Deactivate User',
                        icon: <UserX className="w-3.5 h-3.5 text-red-500" />,
                        danger: true,
                        onClick: () => setDeactivateModalUser(u),
                      });
                    }

                    return (
                      <tr
                        key={u.id}
                        className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                      >
                        <td className="px-5 py-3.5 font-medium text-neutral-900 dark:text-neutral-100">
                          {u.email}
                        </td>
                        <td className="px-5 py-3.5">
                          <Badge variant="default" size="sm">
                            {u.role}
                          </Badge>
                        </td>
                        <td className="px-5 py-3.5">{formatDate(u.createdAt)}</td>
                        <td className="px-5 py-3.5">
                          <Badge variant="success" size="sm" withDot>
                            Active
                          </Badge>
                        </td>
                        <td className="px-5 py-3.5 text-right">
                          {isSelf ? (
                            <span className="text-neutral-400 italic text-[11px]">(You)</span>
                          ) : canManage && dropdownItems.length > 0 ? (
                            <Dropdown
                              align="right"
                              trigger={
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  className="h-8 w-8 p-0 rounded-lg hover:bg-neutral-100 dark:hover:bg-neutral-800"
                                  aria-label="User actions"
                                >
                                  <MoreVertical className="w-4 h-4 text-neutral-500" />
                                </Button>
                              }
                              items={dropdownItems}
                            />
                          ) : (
                            <span className="text-neutral-400 text-[11px]">—</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pending Invitations table */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-neutral-400 dark:text-neutral-500 mb-3 flex items-center gap-1.5">
              <KeyRound className="w-4 h-4" />
              <span>Pending Invitations ({pendingInvites.length})</span>
            </h4>
            {pendingInvites.length === 0 ? (
              <p className="text-xs text-neutral-500 italic p-4 border border-dashed border-neutral-200 dark:border-[#262626] rounded-xl text-center">
                No pending invitations. Click &quot;Invite New User&quot; above to invite teammates.
              </p>
            ) : (
              <div className="overflow-x-auto rounded-xl border border-neutral-200 dark:border-[#262626]">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Invitee Email</th>
                      <th className="px-5 py-3">Assigned Role</th>
                      <th className="px-5 py-3">Created Date</th>
                      <th className="px-5 py-3">Status</th>
                      <th className="px-5 py-3 text-right">Relay Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {pendingInvites.map((inv) => (
                      <tr
                        key={inv.id}
                        className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                      >
                        <td className="px-5 py-3.5 font-medium text-neutral-900 dark:text-neutral-100">
                          {inv.email}
                        </td>
                        <td className="px-5 py-3.5">
                          <Badge variant="default" size="sm">
                            {inv.role}
                          </Badge>
                        </td>
                        <td className="px-5 py-3.5">{formatDate(inv.createdAt)}</td>
                        <td className="px-5 py-3.5">
                          <Badge variant="warning" size="sm" withDot>
                            Pending Activation
                          </Badge>
                        </td>
                        <td className="px-5 py-3.5 text-right">
                          {inv.inviteToken && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleCopyLink(inv.inviteToken!)}
                              leftIcon={
                                copiedToken === inv.inviteToken ? (
                                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                                ) : (
                                  <Copy className="w-3.5 h-3.5" />
                                )
                              }
                            >
                              {copiedToken === inv.inviteToken ? 'Copied URL' : 'Copy Invite URL'}
                            </Button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Deactivated Workspace Members (Historical preservation display) */}
          {deactivatedUsers.length > 0 && (
            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-neutral-400 dark:text-neutral-500 mb-3 flex items-center gap-1.5">
                <UserX className="w-4 h-4" />
                <span>Deactivated Members ({deactivatedUsers.length})</span>
              </h4>
              <div className="overflow-x-auto rounded-xl border border-neutral-200 dark:border-[#262626]">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Member Email</th>
                      <th className="px-5 py-3">Previous Role</th>
                      <th className="px-5 py-3">Joined Date</th>
                      <th className="px-5 py-3">Account Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {deactivatedUsers.map((u) => (
                      <tr
                        key={u.id}
                        className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors opacity-75"
                      >
                        <td className="px-5 py-3.5 font-medium text-neutral-900 dark:text-neutral-100 line-through">
                          {u.email}
                        </td>
                        <td className="px-5 py-3.5">
                          <Badge variant="default" size="sm">
                            {u.role}
                          </Badge>
                        </td>
                        <td className="px-5 py-3.5">{formatDate(u.createdAt)}</td>
                        <td className="px-5 py-3.5">
                          <Badge variant="danger" size="sm" withDot>
                            Deactivated
                          </Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Invite modal */}
      <EmployeeInviteModal
        isOpen={isInviteModalOpen}
        onClose={() => setIsInviteModalOpen(false)}
        onSuccess={handleInviteSuccess}
      />

      {/* Role Change Confirmation Modal */}
      {roleModalUser && (
        <Modal
          isOpen={!!roleModalUser}
          onClose={() => !isSubmitting && setRoleModalUser(null)}
          title="Change User Role"
          description={`Update workspace permissions and role assignment for ${roleModalUser.email}`}
          footer={
            <div className="flex items-center justify-end gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setRoleModalUser(null)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button
                size="sm"
                onClick={handleConfirmChangeRole}
                isLoading={isSubmitting}
                disabled={!selectedNewRole}
              >
                Confirm Role Change
              </Button>
            </div>
          }
        >
          <div className="space-y-4 py-2">
            <div className="rounded-lg border border-neutral-200 bg-neutral-50 dark:border-neutral-800 dark:bg-neutral-900/50 p-3 space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span className="text-neutral-500">Target Member:</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100">
                  {roleModalUser.email}
                </span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-neutral-500">Current Role:</span>
                <Badge variant="default" size="sm">
                  {roleModalUser.role}
                </Badge>
              </div>
            </div>

            <Select
              label="Select New Role"
              value={selectedNewRole}
              onChange={(e) => setSelectedNewRole(e.target.value as Role)}
              disabled={isSubmitting}
              options={availableRolesForModal.map((r) => ({
                value: r,
                label: r,
              }))}
            />

            <div className="rounded-lg border border-amber-200 bg-amber-50/50 dark:border-amber-900/30 dark:bg-amber-950/20 p-3 flex items-start gap-2.5">
              <Shield className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
              <p className="text-xs text-amber-800 dark:text-amber-300">
                Updating this role will immediately invalidate active refresh tokens for this member. The next session refresh or login will pick up the new role.
              </p>
            </div>
          </div>
        </Modal>
      )}

      {/* Deactivate User Confirmation Modal */}
      {deactivateModalUser && (
        <Modal
          isOpen={!!deactivateModalUser}
          onClose={() => !isSubmitting && setDeactivateModalUser(null)}
          title="Deactivate Workspace Member"
          description={`Confirm account deactivation for ${deactivateModalUser.email}`}
          footer={
            <div className="flex items-center justify-end gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setDeactivateModalUser(null)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                size="sm"
                className="bg-red-600 hover:bg-red-700 text-white dark:bg-red-600 dark:hover:bg-red-700"
                onClick={handleConfirmDeactivate}
                isLoading={isSubmitting}
              >
                Deactivate Member
              </Button>
            </div>
          }
        >
          <div className="space-y-4 py-2">
            <div className="rounded-lg border border-red-200 bg-red-50/50 dark:border-red-900/30 dark:bg-red-950/20 p-3.5 flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
              <div className="space-y-1 text-xs text-red-800 dark:text-red-300">
                <p className="font-semibold">Account Access Will Be Terminated</p>
                <p>
                  <strong>{deactivateModalUser.email}</strong> will immediately be prevented from logging in and all active refresh sessions will be permanently revoked.
                </p>
              </div>
            </div>

            <div className="rounded-lg border border-neutral-200 bg-neutral-50 dark:border-neutral-800 dark:bg-neutral-900/50 p-3 text-xs text-neutral-600 dark:text-neutral-400 space-y-1">
              <p className="font-medium text-neutral-800 dark:text-neutral-200">Historical Records Preserved:</p>
              <p>
                All past tasks, leave requests, audit logs, and HRM records linked to this member will be safely retained in the workspace.
              </p>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
