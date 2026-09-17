import React, { useState } from 'react';
import { Check, Copy, KeyRound, Plus, UserPlus } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { EmployeeInviteModal } from '../hrm/EmployeeInviteModal';
import { formatDate } from '../../utils/formatters';

interface LocalInviteRecord {
  email: string;
  role: string;
  token: string;
  createdAt: string;
}

const INITIAL_INVITATIONS: LocalInviteRecord[] = [
  {
    email: 'alex.morgan@company.com',
    role: 'ADMIN',
    token: 'e2a537f8-9a45-4dfc-8d13-c918349514f0',
    createdAt: '2026-09-16T12:00:00.000Z',
  },
  {
    email: 'sarah.connor@company.com',
    role: 'MEMBER',
    token: 'f941ab20-bc42-47d0-a043-41bbd9c19b02',
    createdAt: '2026-09-15T12:00:00.000Z',
  },
];

export const UserManagementPage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [copiedToken, setCopiedToken] = useState<string | null>(null);

  // Local state holding tokens generated in this session
  const [invitations, setInvitations] = useState<LocalInviteRecord[]>(INITIAL_INVITATIONS);

  const handleInviteSuccess = (token: string, email: string, role: string) => {
    setInvitations((prev) => [
      {
        email,
        role,
        token,
        createdAt: new Date().toISOString(),
      },
      ...prev,
    ]);
  };

  const handleCopyLink = (token: string) => {
    const inviteUrl = `${window.location.origin}/accept-invite?token=${token}&tenantId=${user?.tenantId}`;
    navigator.clipboard.writeText(inviteUrl);
    setCopiedToken(token);
    showToast('success', 'Invite URL Copied', 'Share this activation link with your teammate.');
    setTimeout(() => setCopiedToken(null), 2500);
  };

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
              Invite teammates using real backend endpoint <code className="text-zinc-900 dark:text-neutral-100 font-mono">POST /api/users</code>
            </p>
          </div>
          <Button
            size="sm"
            onClick={() => setIsModalOpen(true)}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Invite New User
          </Button>
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

          {/* Invitation relay table */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-neutral-400 dark:text-neutral-500 mb-3 flex items-center gap-1.5">
              <KeyRound className="w-4 h-4" />
              <span>Generated Invitation Tokens (7-Day Expiration)</span>
            </h4>
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
                  {invitations.map((inv) => (
                    <tr
                      key={inv.token}
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
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleCopyLink(inv.token)}
                          leftIcon={
                            copiedToken === inv.token ? (
                              <Check className="w-3.5 h-3.5 text-emerald-600" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )
                          }
                        >
                          {copiedToken === inv.token ? 'Copied URL' : 'Copy Invite URL'}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Invite modal */}
      <EmployeeInviteModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onInviteCreated={handleInviteSuccess}
      />
    </div>
  );
};
