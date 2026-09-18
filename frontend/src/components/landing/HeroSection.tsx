import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, CheckCircle2, Database, Shield, Zap } from 'lucide-react';
import { Button } from '../common/Button';

export const HeroSection: React.FC = () => {
  return (
    <section className="relative pt-32 pb-16 md:pt-40 md:pb-24 overflow-hidden">
      {/* Background Subtle Gradient Grid */}
      <div
        className="absolute inset-0 -z-10 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:24px_24px] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)]"
        aria-hidden="true"
      />

      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-8">
        {/* Eyebrow badge */}
        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-neutral-200 dark:border-[#262626] bg-white/70 dark:bg-[#141414]/70 backdrop-blur-xs text-[11px] font-semibold tracking-wider uppercase text-neutral-700 dark:text-neutral-300 shadow-2xs">
          <span className="flex h-2 w-2 rounded-full bg-[#2563EB] animate-pulse" />
          WORKFORCE &amp; OPERATIONS
        </div>

        {/* Hero Heading */}
        <h1 className="text-4xl sm:text-5xl md:text-6xl font-extrabold tracking-tight text-neutral-900 dark:text-neutral-50 leading-[1.12] max-w-4xl mx-auto">
          Your organization,{' '}
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-neutral-900 via-neutral-700 to-neutral-500 dark:from-white dark:via-neutral-200 dark:to-neutral-400">
            connected in one workspace.
          </span>
        </h1>

        {/* Subtitle */}
        <p className="text-base sm:text-lg md:text-xl text-neutral-600 dark:text-neutral-400 max-w-2xl mx-auto leading-relaxed font-normal">
          Manage people, projects, time, workflows and operations from one secure workspace.
        </p>

        {/* Call to Action Buttons */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
          <Link to="/signup" className="w-full sm:w-auto group">
            <Button
              size="lg"
              className="w-full sm:w-auto px-7 text-sm font-semibold"
              rightIcon={
                <ArrowRight className="w-4 h-4 transition-transform duration-200 group-hover:translate-x-1" />
              }
            >
              Get Started
            </Button>
          </Link>
          <Link to="/login" className="w-full sm:w-auto">
            <Button variant="outline" size="lg" className="w-full sm:w-auto px-7 text-sm">
              Log In
            </Button>
          </Link>
        </div>

        {/* Architecture & Feature Highlights */}
        <div className="pt-6 flex flex-wrap items-center justify-center gap-y-2 gap-x-6 text-xs text-neutral-500 dark:text-neutral-400 font-medium">
          <div className="flex items-center gap-1.5">
            <Database className="w-3.5 h-3.5 text-neutral-800 dark:text-neutral-200" />
            <span>PostgreSQL Schema-per-Tenant</span>
          </div>
          <div className="flex items-center gap-1.5">
            <Shield className="w-3.5 h-3.5 text-neutral-800 dark:text-neutral-200" />
            <span>JWT + RS256 + RBAC</span>
          </div>
          <div className="flex items-center gap-1.5">
            <Zap className="w-3.5 h-3.5 text-neutral-800 dark:text-neutral-200" />
            <span>Automated Stripe Subscriptions</span>
          </div>
          <div className="flex items-center gap-1.5">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
            <span>Zero Data Bleed Across Tenants</span>
          </div>
        </div>
      </div>
    </section>
  );
};
