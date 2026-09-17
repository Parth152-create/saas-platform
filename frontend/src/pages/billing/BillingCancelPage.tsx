import React from 'react';
import { Link } from 'react-router-dom';
import { XCircle, ArrowRight } from 'lucide-react';
import { Button } from '../../components/common/Button';

export const BillingCancelPage: React.FC = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-neutral-50 dark:bg-[#0a0a0a] p-4">
      <div className="max-w-md w-full text-center space-y-6 rounded-2xl border border-neutral-200 bg-white p-8 shadow-lg dark:border-[#262626] dark:bg-[#141414]">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-50 text-amber-600 dark:bg-amber-950/60 dark:text-amber-400 mx-auto shadow-sm">
          <XCircle className="w-8 h-8" />
        </div>

        <div className="space-y-2">
          <h2 className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
            Checkout Cancelled
          </h2>
          <p className="text-xs text-neutral-500 dark:text-neutral-400 leading-relaxed">
            Your payment was not completed and your current workspace plan was not changed.
          </p>
        </div>

        <div className="pt-4 border-t border-neutral-100 dark:border-[#262626]">
          <Link to="/app/billing">
            <Button variant="outline" className="w-full" rightIcon={<ArrowRight className="w-4 h-4" />}>
              Return to Billing Overview
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};
