import { useState } from 'react';
import { Check, FileStack, Plus } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { StatCard } from '../../components/widgets/StatCard';
import { MOCK_CLAIMS, type ClaimItem } from '../../mocks/mockHrmData';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { useToast } from '../../context/ToastContext';

export const ClaimsPage: React.FC = () => {
  const { showToast } = useToast();
  const [claims, setClaims] = useState<ClaimItem[]>(MOCK_CLAIMS);

  const handleAction = (id: string, status: 'Approved' | 'Rejected') => {
    setClaims((prev) =>
      prev.map((c) => (c.id === id ? { ...c, status } : c))
    );
    showToast('success', `Claim ${status}`, 'Claim record updated.');
  };

  const totalClaimAmount = claims.reduce((acc, c) => acc + c.amount, 0);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Employee Expense & Equipment Claims"
        description="Review reimbursement requests, verify invoices, and track operational disbursements."
        actions={
          <Button
            size="sm"
            onClick={() => showToast('info', 'File Claim', 'New expense claim dialog')}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            File New Claim
          </Button>
        }
      />

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          title="Total Open Claims"
          value={claims.length}
          icon={<FileStack className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
        />
        <StatCard
          title="Pending Amount"
          value={formatCurrency(totalClaimAmount * 100)}
          icon={<FileStack className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
          iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
        />
        <StatCard
          title="Approved This Month"
          value={claims.filter((c) => c.status === 'Approved').length}
          icon={<Check className="w-5 h-5 text-emerald-600" />}
          iconBgColor="bg-emerald-50 dark:bg-emerald-950/60"
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Claims Roster</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
              <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                <tr>
                  <th className="px-5 py-3">Claim ID</th>
                  <th className="px-5 py-3">Claimant</th>
                  <th className="px-5 py-3">Category</th>
                  <th className="px-5 py-3">Amount</th>
                  <th className="px-5 py-3">Filed Date</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {claims.map((claim) => (
                  <tr
                    key={claim.id}
                    className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                  >
                    <td className="px-5 py-3.5 font-mono font-semibold text-neutral-900 dark:text-neutral-100">
                      {claim.claimNumber}
                    </td>
                    <td className="px-5 py-3.5 font-medium text-neutral-900 dark:text-neutral-100">
                      {claim.claimant}
                    </td>
                    <td className="px-5 py-3.5">{claim.category}</td>
                    <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                      {formatCurrency(claim.amount * 100)}
                    </td>
                    <td className="px-5 py-3.5">{formatDate(claim.dateFiled)}</td>
                    <td className="px-5 py-3.5">
                      <Badge
                        variant={
                          claim.status === 'Approved'
                            ? 'success'
                            : claim.status === 'Under Review'
                            ? 'warning'
                            : 'info'
                        }
                        size="sm"
                        withDot
                      >
                        {claim.status}
                      </Badge>
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      {claim.status !== 'Approved' && claim.status !== 'Rejected' ? (
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleAction(claim.id, 'Approved')}
                            className="border-emerald-300 text-emerald-700 dark:text-emerald-400"
                          >
                            Approve
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleAction(claim.id, 'Rejected')}
                            className="border-red-300 text-red-700 dark:text-red-400"
                          >
                            Reject
                          </Button>
                        </div>
                      ) : (
                        <span className="text-[11px] text-zinc-400">Processed</span>
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
