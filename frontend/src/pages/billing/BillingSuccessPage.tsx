import React from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CheckCircle2, ArrowRight } from 'lucide-react';
import { Button } from '../../components/common/Button';

export const BillingSuccessPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get('session_id');

  return (
    <div className="min-h-screen flex items-center justify-center bg-neutral-50 dark:bg-[#0a0a0a] p-4">
      <div className="max-w-md w-full text-center space-y-6 rounded-2xl border border-neutral-200 bg-white p-8 shadow-lg dark:border-[#262626] dark:bg-[#141414]">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-400 mx-auto shadow-sm">
          <CheckCircle2 className="w-8 h-8" />
        </div>

        <div className="space-y-2">
          <h2 className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
            Payment Successful!
          </h2>
          <p className="text-xs text-neutral-500 dark:text-neutral-400 leading-relaxed">
            Thank you for upgrading your organization workspace. Your Stripe subscription has been provisioned and synchronized.
          </p>
          {sessionId && (
            <p className="text-[10px] font-mono text-neutral-400 truncate mt-2">
              Session ID: {sessionId}
            </p>
          )}
        </div>

        <div className="pt-4 border-t border-neutral-100 dark:border-[#262626]">
          <Link to="/app/billing">
            <Button className="w-full" rightIcon={<ArrowRight className="w-4 h-4" />}>
              Return to Billing Overview
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};
