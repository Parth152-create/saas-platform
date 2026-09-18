import React, { useCallback, useEffect, useState } from 'react';
import { Check, Copy, KeyRound, Plus, RefreshCw, UserCheck, UserPlus } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { EmployeeInviteModal } from '../hrm/EmployeeInviteModal';
import { usersApi } from '../../api/usersApi';
import type { WorkspaceUser } from '../../api/types';
import { formatDate } from '../../utils/formatters';

export const UserManagementPage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [copiedToken, setCopiedToken] = useState<string | null>(null);

  const [users, setUsers] = useState<WorkspaceUser[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const handleRefresh = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await usersApi.getUsers();
      setUsers(data);
    } catch {
      // If error or non-admin preview
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;
    usersApi.getUsers()
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

  const activeUsers = users.filter((u) => u.status === 'ACTIVE');
  const pendingInvites = users.filter((u) => u.status === 'INVITED');

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <UserPlus className="w-5 h-5 text-zinc-900 dark:text-neutral-100" />
              <span>Workspace User Management & Invitations</span>
            </CardTitle>
            <p className="text-xs text-zinc-500 dark:text-neutral-400 mt-1">
              Manage workspace teammates and issue onboarding invitation links via backend <code className="text-zinc-900 dark:text-neutral-100 font-mono">/api/users</code>
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
              onClick={() => setIsModalOpen(true)}
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
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                  {activeUsers.map((u) => (
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
                    </tr>
                  ))}
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
        </CardContent>
      </Card>

      {/* Invite modal */}
      <EmployeeInviteModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={handleInviteSuccess}
      />
    </div>
  );
};
