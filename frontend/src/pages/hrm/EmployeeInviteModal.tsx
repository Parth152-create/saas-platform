import React, { useState } from 'react';
import { Check, Copy, KeyRound, Mail, ShieldAlert } from 'lucide-react';
import { usersApi } from '../../api/usersApi';
import type { Role } from '../../api/types';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { Select } from '../../components/common/Select';
import { formatDate } from '../../utils/formatters';

export interface EmployeeInviteModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
  onInviteCreated?: (token: string, email: string, role: string) => void;
}

export const EmployeeInviteModal: React.FC<EmployeeInviteModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  onInviteCreated,
}) => {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [email, setEmail] = useState('');
  const [role, setRole] = useState<Role>('USER');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdInvite, setCreatedInvite] = useState<{
    email: string;
    role: Role;
    token: string;
    expiresAt: string;
  } | null>(null);
  const [copied, setCopied] = useState(false);

  const isSuperAdmin = user?.role === 'SUPER_ADMIN';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) {
      setError('Email is required');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await usersApi.inviteUser({
        email: email.trim(),
        role,
      });

      setCreatedInvite({
        email: response.email,
        role: response.role,
        token: response.inviteToken,
        expiresAt: response.expiresAt,
      });

      if (onInviteCreated) {
        onInviteCreated(response.inviteToken, response.email, response.role);
      }

      showToast('success', 'Invitation Created', `Invite token generated for ${response.email}`);
      if (onSuccess) onSuccess();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to generate invitation';
      setError(msg);
      showToast('error', 'Invitation Failed', msg);
    } finally {
      setIsLoading(false);
    }
  };

  const handleReset = () => {
    setEmail('');
    setRole('USER');
    setCreatedInvite(null);
    setError(null);
    setCopied(false);
    onClose();
  };

  const inviteUrl = createdInvite
    ? `${window.location.origin}/accept-invite?token=${encodeURIComponent(
        createdInvite.token
      )}&tenantId=${encodeURIComponent(user?.tenantId || '')}`
    : '';

  const handleCopyLink = () => {
    if (inviteUrl) {
      navigator.clipboard.writeText(inviteUrl);
      setCopied(true);
      showToast('info', 'Copied to Clipboard', 'Invite link ready to share');
      setTimeout(() => setCopied(false), 2500);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleReset}
      title={createdInvite ? 'Teammate Invited Successfully' : 'Invite Teammate'}
      description={
        createdInvite
          ? 'Share this invitation link or token with your new team member.'
          : 'Send an invitation to join your workspace. Backend RBAC escalation controls are enforced.'
      }
      size="md"
    >
      {createdInvite ? (
        <div className="space-y-4">
          <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 dark:border-emerald-900/60 dark:bg-emerald-950/40">
            <div className="flex items-center gap-2 text-emerald-800 dark:text-emerald-200 text-sm font-semibold mb-2">
              <Check className="w-4 h-4 text-emerald-600" />
              <span>Invite generated for {createdInvite.email}</span>
            </div>
            <p className="text-xs text-emerald-700 dark:text-emerald-300">
              Role: <strong>{createdInvite.role}</strong> • Valid until:{' '}
              {formatDate(createdInvite.expiresAt)}
            </p>
          </div>

          <div className="space-y-2">
            <label className="block text-xs font-semibold uppercase tracking-wider text-neutral-700 dark:text-neutral-300">
              Direct Acceptance Link
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={inviteUrl}
                className="w-full rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-xs font-mono text-neutral-700 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-300"
              />
              <Button
                variant="outline"
                size="sm"
                onClick={handleCopyLink}
                leftIcon={copied ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
              >
                {copied ? 'Copied' : 'Copy'}
              </Button>
            </div>
          </div>

          <div className="space-y-2">
            <label className="block text-xs font-semibold uppercase tracking-wider text-neutral-700 dark:text-neutral-300">
              Raw Invite Token
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={createdInvite.token}
                className="w-full rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-xs font-mono text-neutral-700 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-300"
              />
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  navigator.clipboard.writeText(createdInvite.token);
                  showToast('info', 'Token Copied');
                }}
              >
                Copy
              </Button>
            </div>
          </div>

          <div className="p-3 bg-neutral-100 dark:bg-[#1f1f1f] rounded-lg text-xs text-neutral-800 dark:text-neutral-200 flex items-start gap-2">
            <KeyRound className="w-4 h-4 shrink-0 mt-0.5 text-neutral-600 dark:text-neutral-400" />
            <span>
              The invitee can open the acceptance link or use the token on the Accept Invite page to set their password and activate their account.
            </span>
          </div>

          <div className="flex justify-end pt-2">
            <Button onClick={handleReset}>Done</Button>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 rounded-lg text-xs text-red-700 dark:text-red-300 flex items-start gap-2">
              <ShieldAlert className="w-4 h-4 shrink-0 mt-0.5 text-red-600" />
              <span>{error}</span>
            </div>
          )}

          <Input
            label="Teammate Email Address"
            type="email"
            placeholder="colleague@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            leftIcon={<Mail className="w-4 h-4" />}
            required
          />

          <Select
            label="Assigned Workspace Role"
            value={role}
            onChange={(e) => setRole(e.target.value as Role)}
            helperText={
              !isSuperAdmin
                ? 'As an ADMIN, you can invite ADMIN, MANAGER, or USER roles.'
                : 'As a SUPER_ADMIN, you can assign all roles including SUPER_ADMIN.'
            }
          >
            {isSuperAdmin && <option value="SUPER_ADMIN">SUPER_ADMIN (Full Workspace Control)</option>}
            <option value="ADMIN">ADMIN (Workspace Management & Billing)</option>
            <option value="MANAGER">MANAGER (Operations & Approvals)</option>
            <option value="USER">USER (Standard Team Member)</option>
          </Select>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-neutral-100 dark:border-[#262626]">
            <Button variant="secondary" type="button" onClick={handleReset}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isLoading}>
              Send Invitation
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
};
