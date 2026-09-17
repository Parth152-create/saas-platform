import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CheckCircle2, Lock, Mail, Moon, Sun, User } from 'lucide-react';
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

export const SignupPage: React.FC = () => {
  const { signup, googleLogin } = useAuth();
  const { resolvedTheme, toggleTheme } = useTheme();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [fullName, setFullName] = useState('');
  const [tenantId, setTenantId] = useState('');
  const [showCustomTenant, setShowCustomTenant] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  // Field-specific validation states
  const [fieldErrors, setFieldErrors] = useState<{
    fullName?: string;
    email?: string;
    password?: string;
    confirmPassword?: string;
    tenantId?: string;
  }>({});

  // Auto-derive tenant slug from fullName if user hasn't explicitly customized it
  const handleFullNameChange = (val: string) => {
    setFullName(val);
    if (!showCustomTenant) {
      const generated = val
        .toLowerCase()
        .replace(/[^a-z0-9_-]/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 30);
      setTenantId(generated);
    }
  };

  const isPasswordValid = password.length >= 8;
  const isConfirmValid = password.length > 0 && password === confirmPassword;

  const validate = (): boolean => {
    const errors: typeof fieldErrors = {};

    if (!fullName.trim()) {
      errors.fullName = 'Full name is required.';
    }

    if (!email.trim()) {
      errors.email = 'Email address is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = 'Please enter a valid email address.';
    }

    if (!password) {
      errors.password = 'Password is required.';
    } else if (!isPasswordValid) {
      errors.password = 'Password must be at least 8 characters long.';
    }

    if (!confirmPassword) {
      errors.confirmPassword = 'Confirm password is required.';
    } else if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Google OIDC flow using the backend's real endpoint
  const handleGoogleClick = () => {
    let effectiveTenant = tenantId.trim().toLowerCase();
    if (!effectiveTenant && fullName.trim()) {
      effectiveTenant = fullName
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9_-]/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 30);
    }
    if (!effectiveTenant) {
      try {
        effectiveTenant = localStorage.getItem(REMEMBERED_TENANT_KEY) || '';
      } catch {
        effectiveTenant = '';
      }
    }

    if (!effectiveTenant) {
      setServerError('Please enter your full name or workspace name to continue with Google.');
      return;
    }

    setServerError(null);

    if (typeof window !== 'undefined' && window.google?.accounts?.id) {
      setIsGoogleLoading(true);
      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: async (response: { credential: string }) => {
          setIsGoogleLoading(true);
          setServerError(null);
          try {
            await googleLogin(response.credential, effectiveTenant);
            localStorage.setItem(REMEMBERED_TENANT_KEY, effectiveTenant);
            showToast('success', 'Welcome', 'Successfully authenticated with Google');
            navigate('/app/dashboard');
          } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Google authentication failed';
            setServerError(msg);
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
      setServerError('Google Sign-In is initializing. Please wait a moment and try again.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);

    if (!validate()) {
      return;
    }

    // Resolve tenantId complying with backend validation ^[a-z][a-z0-9_-]{0,49}$
    let resolvedTenant = tenantId.trim().toLowerCase();
    if (!resolvedTenant || !/^[a-z]/.test(resolvedTenant)) {
      resolvedTenant = fullName
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9_-]/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 30);
    }
    if (!resolvedTenant || !/^[a-z]/.test(resolvedTenant)) {
      const emailPrefix = email.split('@')[0].toLowerCase().replace(/[^a-z0-9_-]/g, '');
      resolvedTenant = (/^[a-z]/.test(emailPrefix) ? emailPrefix : 'workspace') + '-' + Math.random().toString(36).substring(2, 6);
    }

    setIsLoading(true);

    try {
      await signup({
        tenantId: resolvedTenant,
        email: email.trim(),
        password,
      });

      try {
        localStorage.setItem(REMEMBERED_TENANT_KEY, resolvedTenant);
      } catch {
        // Ignore storage error
      }

      showToast(
        'success',
        'Workspace Provisioned',
        `Tenant "${resolvedTenant}" successfully created. Welcome!`
      );
      navigate('/app/dashboard');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Workspace registration failed';
      setServerError(msg);
      showToast('error', 'Registration Failed', msg);
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
              Create your account
            </h1>
            <p className="text-xs text-neutral-500 dark:text-neutral-400">
              Set up your account to get started.
            </p>
          </div>
        </div>

        {/* Auth Card */}
        <div className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-xs dark:border-[#262626] dark:bg-[#141414] space-y-6">
          {serverError && (
            <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-800 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-300">
              <p className="font-semibold">Unable to create account</p>
              <p className="mt-0.5 leading-relaxed">{serverError}</p>
            </div>
          )}

          {/* Google OIDC Button */}
          <GoogleButton
            onClick={handleGoogleClick}
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

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Input
                label="Full Name"
                placeholder="Enter your full name"
                value={fullName}
                onChange={(e) => handleFullNameChange(e.target.value)}
                error={fieldErrors.fullName}
                leftIcon={<User className="w-4 h-4" />}
                required
              />
              {!showCustomTenant && (
                <button
                  type="button"
                  onClick={() => setShowCustomTenant(true)}
                  className="mt-1 text-[11px] text-neutral-500 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-200 underline cursor-pointer"
                >
                  Customize workspace slug {tenantId ? `(${tenantId})` : ''}
                </button>
              )}
            </div>

            {showCustomTenant && (
              <Input
                label="Workspace / Tenant ID"
                placeholder="e.g. acme"
                value={tenantId}
                onChange={(e) => setTenantId(e.target.value.toLowerCase())}
                error={fieldErrors.tenantId}
                helperText="Must start with a lowercase letter (letters, numbers, hyphens)"
                required
              />
            )}

            <Input
              label="Email Address"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (fieldErrors.email) {
                  setFieldErrors((prev) => ({ ...prev, email: undefined }));
                }
              }}
              error={fieldErrors.email}
              leftIcon={<Mail className="w-4 h-4" />}
              required
            />

            <Input
              label="Password"
              type="password"
              placeholder="Create a password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (fieldErrors.password) {
                  setFieldErrors((prev) => ({ ...prev, password: undefined }));
                }
              }}
              error={fieldErrors.password}
              helperText="Minimum 8 characters"
              leftIcon={<Lock className="w-4 h-4" />}
              required
            />

            <Input
              label="Confirm Password"
              type="password"
              placeholder="Confirm your password"
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                if (fieldErrors.confirmPassword) {
                  setFieldErrors((prev) => ({ ...prev, confirmPassword: undefined }));
                }
              }}
              error={fieldErrors.confirmPassword}
              leftIcon={<Lock className="w-4 h-4" />}
              required
            />

            {/* Validation indicators */}
            <div className="space-y-2 pt-1 text-xs text-neutral-500 dark:text-neutral-400">
              <div className="flex items-center gap-2">
                <CheckCircle2
                  className={`w-3.5 h-3.5 transition-colors ${
                    isPasswordValid ? 'text-emerald-500' : 'text-neutral-300 dark:text-neutral-700'
                  }`}
                />
                <span>8+ character password</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2
                  className={`w-3.5 h-3.5 transition-colors ${
                    isConfirmValid ? 'text-emerald-500' : 'text-neutral-300 dark:text-neutral-700'
                  }`}
                />
                <span>Passwords match</span>
              </div>
            </div>

            <Button
              type="submit"
              className="w-full mt-2"
              size="lg"
              isLoading={isLoading}
              disabled={isLoading}
            >
              Create account
            </Button>
          </form>

          {/* Teammate invitation link */}
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
          Already have an account?{' '}
          <Link
            to="/login"
            className="font-semibold text-neutral-900 hover:underline dark:text-neutral-100"
          >
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
};
