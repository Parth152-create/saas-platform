import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Building, Lock, Mail, Moon, Sun } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { GoogleButton } from '../../components/auth/GoogleButton';

const GOOGLE_CLIENT_ID =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ||
  '906646436149-dkke9514rgicekhqsglpavm8e5iffgug.apps.googleusercontent.com';

const REMEMBERED_TENANT_KEY = 'saas_remembered_tenant';

export const LoginPage: React.FC = () => {
  const { login, googleLogin } = useAuth();
  const { resolvedTheme, toggleTheme } = useTheme();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [tenantId, setTenantId] = useState(() => {
    try {
      return localStorage.getItem(REMEMBERED_TENANT_KEY) || '';
    } catch {
      return '';
    }
  });
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleCustomGoogleClick = () => {
    if (!tenantId.trim()) {
      setError('Please enter your Workspace / Tenant ID before continuing with Google.');
      return;
    }
    setError(null);

    if (window.google?.accounts?.id) {
      setIsGoogleLoading(true);
      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: async (response: { credential: string }) => {
          setIsGoogleLoading(true);
          setError(null);
          try {
            await googleLogin(response.credential, tenantId.trim().toLowerCase());
            if (rememberMe) {
              localStorage.setItem(REMEMBERED_TENANT_KEY, tenantId.trim().toLowerCase());
            }
            showToast('success', 'Welcome', 'Successfully authenticated with Google');
            navigate('/app/dashboard');
          } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Google authentication failed';
            setError(msg);
            showToast('error', 'Google Authentication Failed', msg);
          } finally {
            setIsGoogleLoading(false);
          }
        },
      });
      window.google.accounts.id.prompt((notification) => {
        if (notification.isNotDisplayed()) {
          setIsGoogleLoading(false);
        }
      });
    } else {
      setError('Google Sign-In is initializing. Please wait a moment and try again.');
    }
  };


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tenantId.trim()) {
      setError('Please enter your Workspace / Tenant ID.');
      return;
    }
    if (!email.trim()) {
      setError('Please enter your email address.');
      return;
    }
    if (!password) {
      setError('Please enter your password.');
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      await login({
        tenantId: tenantId.trim().toLowerCase(),
        email: email.trim(),
        password,
      });

      if (rememberMe) {
        localStorage.setItem(REMEMBERED_TENANT_KEY, tenantId.trim().toLowerCase());
      } else {
        localStorage.removeItem(REMEMBERED_TENANT_KEY);
      }

      showToast('success', 'Welcome back', 'Successfully authenticated');
      navigate('/app/dashboard');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Invalid workspace credentials';
      setError(msg);
      showToast('error', 'Authentication Failed', msg);
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
        {/* Brand Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex h-12 w-12 items-center justify-center rounded-xl bg-neutral-950 text-white dark:bg-white dark:text-[#0a0a0a] font-bold text-xl shadow-xs">
            S
          </div>
          <div className="space-y-1">
            <h1 className="text-2xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100">
              Login to Dashboard
            </h1>
            <p className="text-xs text-neutral-500 dark:text-neutral-400">
              Enter your workspace credentials to access your organization
            </p>
          </div>
        </div>

        {/* Auth Card */}
        <div className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-xs dark:border-[#262626] dark:bg-[#141414] space-y-6">
          {error && (
            <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-800 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-300">
              <p className="font-semibold">Authentication Error</p>
              <p className="mt-0.5 leading-relaxed">{error}</p>
            </div>
          )}

          {/* Workspace ID field first so Google auth has tenant context */}
          <div className="space-y-1">
            <Input
              label="Workspace / Tenant ID"
              placeholder="e.g. acme"
              value={tenantId}
              onChange={(e) => {
                setTenantId(e.target.value);
                if (error) setError(null);
              }}
              leftIcon={<Building className="w-4 h-4" />}
              helperText="The unique tenant slug of your organization"
              required
            />
          </div>

          {/* Google OIDC Button */}
          <GoogleButton
            onClick={handleCustomGoogleClick}
            isLoading={isGoogleLoading}
            disabled={isLoading}
          />

          {/* Divider */}
          <div className="relative flex items-center justify-center">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-neutral-200 dark:border-[#262626]" />
            </div>
            <span className="relative bg-white dark:bg-[#141414] px-3 text-[11px] uppercase tracking-wider text-neutral-400">
              or continue with email
            </span>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Email Address"
              type="email"
              placeholder="you@company.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (error) setError(null);
              }}
              leftIcon={<Mail className="w-4 h-4" />}
              required
            />

            <Input
              label="Password"
              type="password"
              placeholder="••••••••••••"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (error) setError(null);
              }}
              leftIcon={<Lock className="w-4 h-4" />}
              required
            />

            <div className="flex items-center justify-between pt-1">
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="rounded border-neutral-300 dark:border-[#262626] text-neutral-950 focus:ring-neutral-900 h-4 w-4 cursor-pointer"
                />
                <span className="text-xs text-neutral-600 dark:text-neutral-400">Remember workspace</span>
              </label>

              <button
                type="button"
                onClick={() =>
                  showToast(
                    'info',
                    'Password Reset',
                    'Contact your organization workspace SUPER_ADMIN to issue a reset token.'
                  )
                }
                className="text-xs font-medium text-neutral-600 hover:text-neutral-950 dark:text-neutral-400 dark:hover:text-neutral-100 transition-colors"
              >
                Forgot password?
              </button>
            </div>

            <Button
              type="submit"
              className="w-full mt-2"
              size="lg"
              isLoading={isLoading}
            >
              Login
            </Button>
          </form>

          {/* Invitation link */}
          <div className="pt-2 text-center border-t border-neutral-100 dark:border-[#262626]">
            <p className="text-xs text-neutral-500 dark:text-neutral-400">
              Received an invite from a teammate?{' '}
              <Link
                to="/accept-invite"
                className="font-semibold text-neutral-900 hover:underline dark:text-neutral-100"
              >
                Accept invite
              </Link>
            </p>
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-neutral-500 dark:text-neutral-400">
          Don&apos;t have an account?{' '}
          <Link
            to="/signup"
            className="font-semibold text-neutral-900 hover:underline dark:text-neutral-100"
          >
            Create account
          </Link>
        </p>
      </div>
    </div>
  );
};
