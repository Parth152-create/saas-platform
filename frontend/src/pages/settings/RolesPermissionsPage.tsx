import React from 'react';
import { Check, Shield, ShieldAlert, X } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';

export const RolesPermissionsPage: React.FC = () => {
  const matrix = [
    { permission: 'Provision & Delete Tenant Schemas', superAdmin: true, admin: false, manager: false, user: false },
    { permission: 'Invite SUPER_ADMIN Teammates', superAdmin: true, admin: false, manager: false, user: false },
    { permission: 'Invite ADMIN, MANAGER, USER', superAdmin: true, admin: true, manager: false, user: false },
    { permission: 'Stripe Checkout & Billing Portal', superAdmin: true, admin: true, manager: false, user: false },
    { permission: 'View Invoices & Billing Summary', superAdmin: true, admin: true, manager: false, user: false },
    { permission: 'Approve Leave & Expense Claims', superAdmin: true, admin: true, manager: true, user: false },
    { permission: 'Configure Work Schedule Models', superAdmin: true, admin: true, manager: true, user: false },
    { permission: 'Log Work Hours & Submit Claims', superAdmin: true, admin: true, manager: true, user: true },
    { permission: 'View Employee Directory', superAdmin: true, admin: true, manager: true, user: true },
  ];

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Shield className="w-5 h-5 text-zinc-900 dark:text-neutral-100" />
            <span>Role-Based Access Control (RBAC) Matrix</span>
          </CardTitle>
          <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
            Authoritative permissions enforced server-side via Spring Security RoleHierarchy
          </p>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="p-4 rounded-xl border border-neutral-200 bg-neutral-50 dark:border-[#262626] dark:bg-[#141414] text-xs text-neutral-900 dark:text-neutral-100 flex items-start gap-3">
            <ShieldAlert className="w-5 h-5 text-neutral-700 dark:text-neutral-300 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">Privilege Escalation Defense Active</p>
              <p className="mt-1 leading-relaxed text-neutral-600 dark:text-neutral-400">
                The backend <code className="font-mono text-neutral-900 dark:text-neutral-100 font-semibold">UserController</code> strictly verifies that only an authenticated <code className="font-mono text-neutral-900 dark:text-neutral-100 font-semibold">SUPER_ADMIN</code> can invite new <code className="font-mono text-neutral-900 dark:text-neutral-100 font-semibold">SUPER_ADMIN</code> accounts. Attempting escalation returns HTTP 403 Forbidden.
              </p>
            </div>
          </div>

          <div className="overflow-x-auto rounded-xl border border-neutral-200 dark:border-[#262626]">
            <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
              <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                <tr>
                  <th className="px-5 py-3">Permission / Capability</th>
                  <th className="px-5 py-3 text-center">SUPER_ADMIN</th>
                  <th className="px-5 py-3 text-center">ADMIN</th>
                  <th className="px-5 py-3 text-center">MANAGER</th>
                  <th className="px-5 py-3 text-center">USER</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {matrix.map((row, idx) => (
                  <tr key={idx} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a]">
                    <td className="px-5 py-3.5 font-medium text-neutral-800 dark:text-neutral-200">
                      {row.permission}
                    </td>
                    <td className="px-5 py-3.5 text-center">
                      {row.superAdmin ? (
                        <Check className="w-4 h-4 text-emerald-500 mx-auto" />
                      ) : (
                        <X className="w-4 h-4 text-zinc-300 dark:text-neutral-600 mx-auto" />
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-center">
                      {row.admin ? (
                        <Check className="w-4 h-4 text-emerald-500 mx-auto" />
                      ) : (
                        <X className="w-4 h-4 text-zinc-300 dark:text-neutral-600 mx-auto" />
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-center">
                      {row.manager ? (
                        <Check className="w-4 h-4 text-emerald-500 mx-auto" />
                      ) : (
                        <X className="w-4 h-4 text-zinc-300 dark:text-neutral-600 mx-auto" />
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-center">
                      {row.user ? (
                        <Check className="w-4 h-4 text-emerald-500 mx-auto" />
                      ) : (
                        <X className="w-4 h-4 text-zinc-300 dark:text-neutral-600 mx-auto" />
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
