import React from 'react';
import { BarChart3, Download, FileSpreadsheet, TrendingUp } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { SimpleBarChart, SimpleDonutChart } from '../../components/widgets/SimpleChart';
import { useToast } from '../../context/ToastContext';

export const ReportsPage: React.FC = () => {
  const { showToast } = useToast();

  const reportFiles = [
    { name: 'Monthly Workforce Cost & Billable Utilization Q3-2026.pdf', date: 'Sep 15, 2026', size: '3.4 MB' },
    { name: 'Organization Absence & PTO Accrual Summary.xlsx', date: 'Sep 01, 2026', size: '1.2 MB' },
    { name: 'Tenant Resource & DB Storage Allocation Report.csv', date: 'Aug 31, 2026', size: '450 KB' },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Analytics & HR Reports"
        description="Consolidated workforce analytics, payroll forecasting, and compliance audit exports."
        actions={
          <Button
            size="sm"
            onClick={() => showToast('info', 'Generating Report', 'Exporting consolidated metrics')}
            leftIcon={<Download className="w-4 h-4" />}
          >
            Export Comprehensive Report
          </Button>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-zinc-900 dark:text-neutral-100" />
              <span>Headcount Growth vs Plan</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <SimpleBarChart
              data={[
                { label: 'Q1', value: 32 },
                { label: 'Q2', value: 39 },
                { label: 'Q3', value: 44 },
                { label: 'Q4 (Target)', value: 50 },
              ]}
              height={180}
              valueFormatter={(v) => `${v} staff`}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="w-4 h-4 text-zinc-900 dark:text-neutral-100" />
              <span>Department Cost Allocation</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-2">
            <SimpleDonutChart
              segments={[
                { label: 'Engineering', value: 45, color: '#18181b', darkColor: '#f4f4f5' },
                { label: 'Product & Design', value: 20, color: '#52525b', darkColor: '#d4d4d8' },
                { label: 'Operations', value: 15, color: '#71717a', darkColor: '#a1a1aa' },
                { label: 'Sales & Marketing', value: 20, color: '#a1a1aa', darkColor: '#71717a' },
              ]}
              centerText="$248k"
              centerSubtext="Monthly Run Rate"
              size={170}
            />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Generated Operational Reports</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
            {reportFiles.map((f, i) => (
              <div key={i} className="flex items-center justify-between p-4 hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a]">
                <div className="flex items-center gap-3">
                  <FileSpreadsheet className="w-5 h-5 text-neutral-900 dark:text-neutral-100 shrink-0" />
                  <div>
                    <p className="text-xs font-semibold text-neutral-900 dark:text-neutral-100">{f.name}</p>
                    <p className="text-[11px] text-neutral-400">Generated {f.date} • {f.size}</p>
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<Download className="w-3.5 h-3.5" />}
                  onClick={() => showToast('success', 'Download Started', f.name)}
                >
                  Download
                </Button>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
