import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ShieldCheck, Sparkles } from 'lucide-react';
import { Button } from '../common/Button';
import { NexaMark } from '../common/NexaLogo';

export const FinalCTA: React.FC = () => {
  return (
    <section className="py-24 bg-white dark:bg-[#0a0a0a] border-b border-neutral-200 dark:border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="relative rounded-3xl p-8 sm:p-14 md:p-16 bg-neutral-950 text-white dark:bg-[#121212] border border-neutral-800 dark:border-[#262626] overflow-hidden shadow-2xl text-center">
          {/* Subtle background glow effect */}
          <div className="absolute inset-0 bg-radial from-neutral-800/20 via-transparent to-transparent pointer-events-none" />

          <div className="relative max-w-3xl mx-auto">
            <div className="flex justify-center mb-5">
              <NexaMark size={40} theme="dark" />
            </div>

            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-neutral-800 text-neutral-200 border border-neutral-700 mb-6">
              <Sparkles className="w-3.5 h-3.5" />
              Unified Enterprise Workspace
            </div>

            <h2 className="text-3xl sm:text-4xl md:text-5xl font-extrabold tracking-tight text-white leading-tight">
              Bring your people, projects, and operations together.
            </h2>

            <p className="mt-6 text-base sm:text-lg text-neutral-400 max-w-2xl mx-auto leading-relaxed">
              Eliminate software sprawl. Protect your enterprise data with schema-per-tenant
              PostgreSQL isolation, cryptographic JWT security, and modern workforce tools.
            </p>

            <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link to="/signup" className="w-full sm:w-auto group">
                <Button
                  variant="secondary"
                  size="lg"
                  rightIcon={
                    <ArrowRight className="w-4 h-4 transition-transform duration-200 group-hover:translate-x-1" />
                  }
                  className="w-full sm:w-auto px-8"
                >
                  Get Started
                </Button>
              </Link>
              <Link to="/login" className="w-full sm:w-auto">
                <Button
                  variant="ghost"
                  size="lg"
                  className="w-full sm:w-auto text-neutral-300 hover:text-white hover:bg-neutral-900 border border-neutral-800"
                >
                  Log In
                </Button>
              </Link>
            </div>

            <div className="mt-8 flex items-center justify-center gap-2 text-xs text-neutral-400">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Dedicated schema per tenant &bull; Instant provisioning &bull; No card required for Starter</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
