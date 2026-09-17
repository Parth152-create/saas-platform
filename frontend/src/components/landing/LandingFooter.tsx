import React from 'react';
import { Link } from 'react-router-dom';
import { Moon, Sun } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';

export const LandingFooter: React.FC = () => {
  const { resolvedTheme, toggleTheme } = useTheme();

  return (
    <footer className="bg-white dark:bg-[#0a0a0a] text-neutral-600 dark:text-neutral-400 border-t border-neutral-200 dark:border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-10">
          {/* Brand Col */}
          <div className="lg:col-span-2">
            <Link to="/" className="flex items-center gap-3 select-none">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-neutral-950 text-white dark:bg-white dark:text-[#0a0a0a] font-bold text-sm">
                S
              </div>
              <div className="flex flex-col">
                <span className="text-sm font-bold tracking-tight text-neutral-900 dark:text-neutral-100">
                  SaaS Platform
                </span>
                <span className="text-[10px] text-neutral-500 font-medium tracking-wide uppercase">
                  Enterprise Workforce &amp; HRM
                </span>
              </div>
            </Link>
            <p className="mt-4 text-xs leading-relaxed max-w-sm text-neutral-500 dark:text-neutral-400">
              A modern, multi-tenant workforce management platform. Built on schema-per-tenant
              PostgreSQL isolation, cryptographic JWT authorization, and Stripe billing.
            </p>
            <div className="mt-6">
              <button
                type="button"
                onClick={toggleTheme}
                className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-neutral-200 dark:border-[#262626] text-xs font-medium text-neutral-700 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-[#1f1f1f] transition-colors cursor-pointer"
              >
                {resolvedTheme === 'dark' ? (
                  <>
                    <Sun className="w-3.5 h-3.5" /> Light Mode
                  </>
                ) : (
                  <>
                    <Moon className="w-3.5 h-3.5" /> Dark Mode
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Product Links */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-neutral-900 dark:text-neutral-200 font-mono mb-4">
              Platform
            </h4>
            <ul className="space-y-2.5 text-xs">
              <li>
                <a href="#product" className="hover:text-neutral-950 dark:hover:text-neutral-100 transition-colors">
                  Product Overview
                </a>
              </li>
              <li>
                <a href="#platform" className="hover:text-neutral-950 dark:hover:text-neutral-100 transition-colors">
                  Core Modules
                </a>
              </li>
              <li>
                <a href="#features" className="hover:text-neutral-950 dark:hover:text-neutral-100 transition-colors">
                  Workforce &amp; HRM
                </a>
              </li>
              <li>
                <a href="#security" className="hover:text-neutral-950 dark:hover:text-neutral-100 transition-colors">
                  Security Architecture
                </a>
              </li>
              <li>
                <a href="#pricing" className="hover:text-neutral-950 dark:hover:text-neutral-100 transition-colors">
                  Subscription Plans
                </a>
              </li>
            </ul>
          </div>

          {/* Access Links */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-neutral-900 dark:text-neutral-200 font-mono mb-4">
              Access &amp; Auth
            </h4>
            <ul className="space-y-2.5 text-xs">
              <li>
                <Link to="/login" className="hover:text-neutral-950 dark:hover:text-neutral-100 transition-colors">
                  Workspace Login
                </Link>
              </li>
              <li>
                <Link to="/signup" className="hover:text-neutral-950 dark:hover:text-neutral-100 transition-colors">
                  Create Workspace
                </Link>
              </li>
              <li>
                <Link to="/accept-invite" className="hover:text-neutral-950 dark:hover:text-neutral-100 transition-colors">
                  Accept Invitation
                </Link>
              </li>
              <li>
                <Link to="/app/dashboard" className="hover:text-neutral-950 dark:hover:text-neutral-100 transition-colors">
                  Console Dashboard
                </Link>
              </li>
            </ul>
          </div>

          {/* Architecture / Legal */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-neutral-900 dark:text-neutral-200 font-mono mb-4">
              Architecture
            </h4>
            <ul className="space-y-2.5 text-xs text-neutral-500 dark:text-neutral-400">
              <li>PostgreSQL Schema Isolation</li>
              <li>Stateless JWT + Redis Rotation</li>
              <li>Stripe Webhook Signature Verification</li>
              <li>Role-Based Access Control (RBAC)</li>
              <li>Spring Boot 3 + React 19 Engine</li>
            </ul>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="mt-12 pt-8 border-t border-neutral-200/80 dark:border-[#262626] flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-neutral-500 dark:text-neutral-400">
          <div>
            &copy; 2026 SaaS Platform. All rights reserved. Built with schema-per-tenant isolation.
          </div>
          <div className="flex items-center gap-6">
            <span className="text-[11px] font-mono">v1.0.0-PROD</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
