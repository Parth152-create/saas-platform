import { Building2, Plus } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card } from '../../components/common/Card';
import { MOCK_DEPARTMENTS } from '../../mocks/mockHrmData';
import { useToast } from '../../context/ToastContext';

export const TeamsPage: React.FC = () => {
  const { showToast } = useToast();

  return (
    <div className="space-y-6">
      <PageHeader
        title="Teams & Departments"
        description="Workforce organization hierarchies, leadership assignments, and department budgets."
        actions={
          <Button
            size="sm"
            onClick={() => showToast('info', 'Create Department', 'Department creator dialog')}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Create Department
          </Button>
        }
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {MOCK_DEPARTMENTS.map((dept) => (
          <Card key={dept.name} hoverEffect className="p-6 flex flex-col justify-between space-y-4">
            <div>
              <div className="flex items-center justify-between mb-3">
                <div className="p-2 rounded-xl bg-neutral-100 text-neutral-900 dark:bg-[#1f1f1f] dark:text-neutral-100">
                  <Building2 className="w-5 h-5" />
                </div>
                <Badge variant="primary" size="sm">
                  {dept.headCount} Staff
                </Badge>
              </div>
              <h3 className="text-base font-bold text-neutral-900 dark:text-neutral-100">
                {dept.name}
              </h3>
              <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                Department Lead: <strong className="text-neutral-800 dark:text-neutral-200">{dept.lead}</strong>
              </p>
            </div>

            <div className="space-y-2 pt-3 border-t border-neutral-100 dark:border-[#262626]">
              <div className="flex justify-between text-xs">
                <span className="text-neutral-500">Budget Utilization</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100">
                  {dept.budgetUtilization}%
                </span>
              </div>
              <div className="w-full bg-neutral-100 dark:bg-[#1f1f1f] h-2 rounded-full overflow-hidden">
                <div
                  className="bg-neutral-900 dark:bg-white h-full rounded-full"
                  style={{ width: `${dept.budgetUtilization}%` }}
                />
              </div>
            </div>

            <Button
              variant="outline"
              size="sm"
              className="w-full"
              onClick={() => showToast('info', dept.name, `Viewing department roster`)}
            >
              View Team Members
            </Button>
          </Card>
        ))}
      </div>
    </div>
  );
};
