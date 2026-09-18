import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Building, KeyRound, Lock, Moon, Sun, UserCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { NexaMark } from '../../components/common/NexaLogo';

export const AcceptInvitePage: React.FC = () => {
  const { acceptInvite } = useAuth();
  const { resolvedTheme, toggleTheme } = useTheme();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [tenantId, setTenantId] = useState(() => searchParams.get('tenantId') || '');
  const [token, setToken] = useState(() => searchParams.get('token') || '');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tenantId.trim()) {
      setError('Workspace / Tenant ID is required.');
      return;
    }
    if (!token.trim()) {
      setError('Invitation token is required.');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      await acceptInvite({
        tenantId: tenantId.trim().toLowerCase(),
        token: token.trim(),
        password,
      });
      showToast('success', 'Invitation Accepted', 'Account activated! Welcome to your workspace.');
      navigate('/app/dashboard');
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : 'Invalid or expired invitation token.';
      setError(msg);
      showToast('error', 'Invitation Acceptance Failed', msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-neutral-50 dark:bg-[#0a0a0a] px-4 py-12 transition-colors relative">
      {/* Top right theme toggle */}
      <div className="absolute top-6 right-6">
        <button
          type="button"
          onClick={toggleTheme}
          className="p-2.5 rounded-lg border border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#141414] text-neutral-700 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-[#1f1f1f] transition-colors cursor-pointer"
          aria-label={`Switch to ${resolvedTheme === 'dark' ? 'light' : 'dark'} mode`}
          title={`Switch to ${resolvedTheme === 'dark' ? 'light' : 'dark'} mode`}
        >
          {resolvedTheme === 'dark' ? (
            <Sun className="w-4 h-4 text-neutral-100" />
          ) : (
            <Moon className="w-4 h-4 text-neutral-800" />
          )}
        </button>
      </div>

      <div className="w-full max-w-md space-y-6">
        {/* Brand */}
        <div className="text-center space-y-3">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-neutral-100 dark:bg-[#181818] border border-neutral-200/80 dark:border-[#262626] shadow-xs">
            <NexaMark size={32} />
          </div>
          <div className="space-y-0.5">
            <h2 className="text-xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100">
              Nexa
            </h2>
            <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-neutral-400 dark:text-neutral-500">
              WORKFORCE &amp; OPERATIONS
            </p>
          </div>
          <div className="pt-2 space-y-1">
            <h1 className="text-2xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100">
              Accept Teammate Invitation
            </h1>
            <p className="text-xs text-neutral-500 dark:text-neutral-400">
              Set your workspace password to activate your account
            </p>
          </div>
        </div>

        {/* Card */}
        <div className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-xs dark:border-[#262626] dark:bg-[#141414] space-y-6">
          {error && (
            <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-800 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-300">
              <p className="font-semibold">Unable to accept invitation</p>
              <p className="mt-0.5 leading-relaxed">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Organization / Workspace ID"
              placeholder="e.g. acme"
              value={tenantId}
              onChange={(e) => setTenantId(e.target.value)}
              leftIcon={<Building className="w-4 h-4" />}
              required
            />

            <Input
              label="Invitation Token"
              placeholder="Paste UUID invite token"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              leftIcon={<KeyRound className="w-4 h-4" />}
              helperText="Supplied by your workspace administrator"
              required
            />

            <Input
              label="Create Your Password"
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              leftIcon={<Lock className="w-4 h-4" />}
              helperText="Minimum 8 characters"
              required
            />

            <Button
              type="submit"
              className="w-full mt-2"
              size="lg"
              isLoading={isLoading}
              leftIcon={<UserCheck className="w-4 h-4" />}
            >
              Activate Account & Enter
            </Button>
          </form>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-neutral-500 dark:text-neutral-400">
          Already active?{' '}
          <Link
            to="/login"
            className="font-semibold text-neutral-900 hover:underline dark:text-neutral-100"
          >
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
};
