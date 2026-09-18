import React from 'react';
import { Shield, Database, Lock, Key, RefreshCw, FileCheck2 } from 'lucide-react';

export const SecuritySection: React.FC = () => {
  const securityPillars = [
    {
      icon: Database,
      title: 'Schema-per-Tenant PostgreSQL Isolation',
      desc: 'Each organization operates on its own dedicated PostgreSQL schema. Database-level boundaries prevent cross-tenant data access.',
    },
    {
      icon: Lock,
      title: 'Cryptographic JWT Authentication',
      desc: 'Stateless JSON Web Tokens with cryptographically signed claims ensure tamper-proof authentication across all API endpoints.',
    },
    {
      icon: Key,
      title: 'Hierarchical Role-Based Access Control',
      desc: 'Granular RBAC boundaries strictly enforce least-privilege policies across Super Admin, Admin, Manager, and User roles.',
    },
    {
      icon: RefreshCw,
      title: 'Refresh Token Rotation',
      desc: 'Single-use refresh token exchange with Redis session stores protects users against replay attacks and token interception.',
    },
    {
      icon: Shield,
      title: 'Stripe Webhook Signature Verification',
      desc: 'All billing events and checkout sessions verify HMAC signatures before executing tenant subscription state updates.',
    },
    {
      icon: FileCheck2,
      title: 'Automated Cross-Tenant Isolation Tests',
      desc: 'Continuous regression test suites systematically verify that tenant boundary leakage is mathematically and architecturally impossible.',
    },
  ];

  return (
    <section id="security" className="py-24 bg-neutral-50 dark:bg-[#0d0d0d] border-b border-neutral-200 dark:border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="max-w-3xl mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-neutral-200/70 text-neutral-800 dark:bg-[#1a1a1a] dark:text-neutral-300 border border-neutral-300/80 dark:border-[#262626] mb-4">
            Security & Architecture
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100 leading-tight">
            Built for organizations that care about their data.
          </h2>
          <p className="mt-4 text-base sm:text-lg text-neutral-600 dark:text-neutral-400">
            Enterprise data isolation is not an afterthought or a database filter flag. It is
            enforced at the database schema layer and verified by automated regression tests.
          </p>
        </div>

        {/* Visual Multi-Tenancy Architecture Diagram */}
        <div className="mb-16 p-8 sm:p-10 rounded-2xl bg-white dark:bg-[#121212] border border-neutral-200 dark:border-[#262626] shadow-xs">
          <div className="text-center max-w-xl mx-auto mb-10">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-neutral-500 dark:text-neutral-400">
              Data Boundary Model
            </span>
            <h3 className="text-lg sm:text-xl font-bold text-neutral-900 dark:text-neutral-100 mt-1">
              Physical Schema Separation per Organization
            </h3>
            <p className="text-xs text-neutral-600 dark:text-neutral-400 mt-2">
              Incoming requests resolve the tenant context dynamically. Database queries run only
              within that tenant’s private schema.
            </p>
          </div>

          {/* Architecture Tree */}
          <div className="max-w-3xl mx-auto flex flex-col items-center">
            {/* Top Node */}
            <div className="px-6 py-3 rounded-xl bg-neutral-900 text-white dark:bg-white dark:text-neutral-900 font-bold text-xs tracking-wide shadow-xs border border-neutral-800 dark:border-neutral-200">
              Nexa Platform Engine
            </div>

            {/* Vertical Connector */}
            <div className="w-px h-6 bg-neutral-300 dark:bg-[#333]" />

            {/* Horizontal Branch Bar */}
            <div className="relative w-full max-w-xl">
              <div className="h-px bg-neutral-300 dark:bg-[#333] w-full" />
              <div className="absolute left-1/6 -top-1 w-2 h-2 rounded-full bg-neutral-400 dark:bg-[#555]" />
              <div className="absolute left-1/2 -top-1 -translate-x-1/2 w-2 h-2 rounded-full bg-neutral-400 dark:bg-[#555]" />
              <div className="absolute right-1/6 -top-1 w-2 h-2 rounded-full bg-neutral-400 dark:bg-[#555]" />
            </div>

            {/* Tenant Nodes */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 w-full max-w-xl mt-4">
              {/* Tenant A */}
              <div className="p-4 rounded-xl bg-neutral-50 dark:bg-[#181818] border border-neutral-200 dark:border-[#262626] text-center">
                <div className="text-xs font-bold text-neutral-900 dark:text-neutral-100">
                  Acme Corp
                </div>
                <div className="text-[10px] font-mono text-neutral-500 dark:text-neutral-400 mt-0.5">
                  tenant_acme
                </div>
                <div className="mt-3 py-1 px-2 rounded bg-emerald-100/70 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300 text-[10px] font-mono font-medium border border-emerald-200 dark:border-emerald-800/40">
                  schema_acme
                </div>
                <div className="mt-2 text-[10px] text-neutral-500 dark:text-neutral-400">
                  Isolated Personnel &amp; Financials
                </div>
              </div>

              {/* Tenant B */}
              <div className="p-4 rounded-xl bg-neutral-50 dark:bg-[#181818] border border-neutral-200 dark:border-[#262626] text-center">
                <div className="text-xs font-bold text-neutral-900 dark:text-neutral-100">
                  Starlight Labs
                </div>
                <div className="text-[10px] font-mono text-neutral-500 dark:text-neutral-400 mt-0.5">
                  tenant_starlight
                </div>
                <div className="mt-3 py-1 px-2 rounded bg-emerald-100/70 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300 text-[10px] font-mono font-medium border border-emerald-200 dark:border-emerald-800/40">
                  schema_starlight
                </div>
                <div className="mt-2 text-[10px] text-neutral-500 dark:text-neutral-400">
                  Isolated Personnel &amp; Financials
                </div>
              </div>

              {/* Tenant C */}
              <div className="p-4 rounded-xl bg-neutral-50 dark:bg-[#181818] border border-neutral-200 dark:border-[#262626] text-center">
                <div className="text-xs font-bold text-neutral-900 dark:text-neutral-100">
                  Apex Dynamics
                </div>
                <div className="text-[10px] font-mono text-neutral-500 dark:text-neutral-400 mt-0.5">
                  tenant_apex
                </div>
                <div className="mt-3 py-1 px-2 rounded bg-emerald-100/70 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300 text-[10px] font-mono font-medium border border-emerald-200 dark:border-emerald-800/40">
                  schema_apex
                </div>
                <div className="mt-2 text-[10px] text-neutral-500 dark:text-neutral-400">
                  Isolated Personnel &amp; Financials
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Security Feature Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {securityPillars.map((item) => {
            const Icon = item.icon;
            return (
              <div
                key={item.title}
                className="p-6 rounded-xl bg-white dark:bg-[#141414] border border-neutral-200 dark:border-[#262626] shadow-xs flex flex-col justify-between"
              >
                <div>
                  <div className="p-2.5 w-fit rounded-lg bg-neutral-100 dark:bg-[#1c1c1c] text-neutral-900 dark:text-neutral-100 mb-4 border border-neutral-200 dark:border-[#262626]">
                    <Icon className="w-4 h-4" />
                  </div>
                  <h3 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100 mb-2">
                    {item.title}
                  </h3>
                  <p className="text-xs text-neutral-600 dark:text-neutral-400 leading-relaxed">
                    {item.desc}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
