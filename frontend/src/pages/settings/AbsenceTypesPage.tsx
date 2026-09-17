import React from 'react';
import { Plus, UserCheck } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { MOCK_ABSENCE_TYPES } from '../../mocks/mockHrmData';
import { useToast } from '../../context/ToastContext';

export const AbsenceTypesPage: React.FC = () => {
  const { showToast } = useToast();

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle className="flex items-center gap-2">
            <UserCheck className="w-5 h-5 text-zinc-900 dark:text-neutral-100" />
            <span>Absence Types & Time-Off Policies</span>
          </CardTitle>
          <p className="text-xs text-zinc-500 mt-1">
            Define organizational quotas, paid vs unpaid allocations, and approval rules
          </p>
        </div>
        <Button
          size="sm"
          onClick={() => showToast('info', 'New Absence Type', 'Policy modal opened')}
          leftIcon={<Plus className="w-4 h-4" />}
        >
          Add Policy
        </Button>
      </CardHeader>
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
            <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
              <tr>
                <th className="px-5 py-3">Absence Policy</th>
                <th className="px-5 py-3">Allowance (Days)</th>
                <th className="px-5 py-3">Compensation</th>
                <th className="px-5 py-3">Approval Workflow</th>
                <th className="px-5 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
              {MOCK_ABSENCE_TYPES.map((a) => (
                <tr key={a.id} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a]">
                  <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                    {a.name}
                  </td>
                  <td className="px-5 py-3.5 font-mono">{a.allowanceDays} days</td>
                  <td className="px-5 py-3.5">
                    <Badge variant={a.paid ? 'success' : 'default'} size="sm">
                      {a.paid ? 'Paid' : 'Unpaid'}
                    </Badge>
                  </td>
                  <td className="px-5 py-3.5">
                    {a.requiresApproval ? 'Manager Approval' : 'Automatic Approval'}
                  </td>
                  <td className="px-5 py-3.5 text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => showToast('info', 'Edit Policy', a.name)}
                    >
                      Edit
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );
};
