import { Clock, Plus, Users } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card } from '../../components/common/Card';
import { MOCK_SCHEDULE_MODELS } from '../../mocks/mockHrmData';
import { useToast } from '../../context/ToastContext';

export const WorkSchedulesPage: React.FC = () => {
  const { showToast } = useToast();

  return (
    <div className="space-y-6">
      <PageHeader
        title="Work Schedule Models"
        description="Configure standard weekly shifts, flexible core hours, and employee roster assignments."
        actions={
          <Button
            size="sm"
            onClick={() => showToast('info', 'Schedule Builder', 'Schedule model builder opened')}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Create Schedule Model
          </Button>
        }
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {MOCK_SCHEDULE_MODELS.map((model) => (
          <Card key={model.id} hoverEffect className="p-6 flex flex-col justify-between space-y-5">
            <div>
              <div className="flex items-center justify-between mb-3">
                <Badge
                  variant={model.type === 'Fixed' ? 'primary' : 'default'}
                  size="sm"
                >
                  {model.type} Model
                </Badge>
                <span className="text-xs text-zinc-400 font-mono">{model.weeklyHours} hrs/wk</span>
              </div>
              <h3 className="text-base font-semibold text-neutral-900 dark:text-neutral-100">
                {model.name}
              </h3>
              <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                {model.daysPerWeek} days per week with core collaboration windows.
              </p>
            </div>

            <div className="space-y-3 pt-3 border-t border-neutral-100 dark:border-[#262626]">
              <div className="flex items-center justify-between text-xs">
                <span className="text-neutral-500 flex items-center gap-1.5">
                  <Users className="w-4 h-4 text-neutral-400" />
                  Assigned Staff
                </span>
                <span className="font-semibold text-neutral-800 dark:text-neutral-200">
                  {model.assignedEmployees} employees
                </span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-zinc-500 flex items-center gap-1.5">
                  <Clock className="w-4 h-4 text-zinc-400" />
                  Daily Windows
                </span>
                <span className="font-semibold text-zinc-800 dark:text-neutral-200">
                  09:00 AM – 05:30 PM
                </span>
              </div>
            </div>

            <Button
              variant="outline"
              size="sm"
              className="w-full"
              onClick={() => showToast('info', 'Schedule Details', `Editing ${model.name}`)}
            >
              Configure Model Rules
            </Button>
          </Card>
        ))}
      </div>
    </div>
  );
};
