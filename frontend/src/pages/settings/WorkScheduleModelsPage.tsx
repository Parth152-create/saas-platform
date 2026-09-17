import React from 'react';
import { Calendar, Plus } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { MOCK_SCHEDULE_MODELS } from '../../mocks/mockHrmData';
import { useToast } from '../../context/ToastContext';

export const WorkScheduleModelsPage: React.FC = () => {
  const { showToast } = useToast();

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="w-5 h-5 text-zinc-900 dark:text-neutral-100" />
            <span>Configured Work Schedule Models</span>
          </CardTitle>
          <p className="text-xs text-zinc-500 mt-1">
            Weekly core hour templates for workforce assignments
          </p>
        </div>
        <Button
          size="sm"
          onClick={() => showToast('info', 'New Schedule Model', 'Schedule modal opened')}
          leftIcon={<Plus className="w-4 h-4" />}
        >
          Add Model
        </Button>
      </CardHeader>
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
            <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
              <tr>
                <th className="px-5 py-3">Schedule Name</th>
                <th className="px-5 py-3">Type</th>
                <th className="px-5 py-3">Weekly Hours</th>
                <th className="px-5 py-3">Assigned Staff</th>
                <th className="px-5 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
              {MOCK_SCHEDULE_MODELS.map((m) => (
                <tr key={m.id} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a]">
                  <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                    {m.name}
                  </td>
                  <td className="px-5 py-3.5">
                    <Badge variant={m.type === 'Fixed' ? 'primary' : 'default'} size="sm">
                      {m.type}
                    </Badge>
                  </td>
                  <td className="px-5 py-3.5 font-mono">{m.weeklyHours} hrs/wk</td>
                  <td className="px-5 py-3.5">{m.assignedEmployees} employees</td>
                  <td className="px-5 py-3.5 text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => showToast('info', 'Edit Model', m.name)}
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
