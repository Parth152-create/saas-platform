import React from 'react';
import { Calendar as CalendarIcon, ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { INITIAL_EMPLOYEES } from '../../mocks/mockHrmData';
import { useToast } from '../../context/ToastContext';

export const SchedulePage: React.FC = () => {
  const { showToast } = useToast();

  const daysOfWeek = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Weekly Master Work Schedule"
        description="View team availability, active on-site rosters, and remote collaboration shifts."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              leftIcon={<ChevronLeft className="w-3.5 h-3.5" />}
              onClick={() => showToast('info', 'Previous Week', 'Navigated to prior week')}
            >
              Prev
            </Button>
            <span className="text-xs font-semibold px-2">Sep 14 – Sep 20, 2026</span>
            <Button
              variant="outline"
              size="sm"
              rightIcon={<ChevronRight className="w-3.5 h-3.5" />}
              onClick={() => showToast('info', 'Next Week', 'Navigated to next week')}
            >
              Next
            </Button>
            <Button
              size="sm"
              leftIcon={<Plus className="w-4 h-4" />}
              onClick={() => showToast('info', 'Add Shift', 'Shift assignment dialog')}
            >
              Assign Shift
            </Button>
          </div>
        }
      />

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <CalendarIcon className="w-4 h-4 text-zinc-900 dark:text-neutral-100" />
            <span>Shift Allocation Grid</span>
          </CardTitle>
          <Badge variant="primary" size="sm">
            Sprint 24
          </Badge>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
              <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                <tr>
                  <th className="px-4 py-3 min-w-36">Staff Member</th>
                  {daysOfWeek.map((day) => (
                    <th key={day} className="px-3 py-3 min-w-28 text-center">
                      {day}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {INITIAL_EMPLOYEES.map((emp) => (
                  <tr key={emp.id} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a]">
                    <td className="px-4 py-3 font-semibold text-neutral-900 dark:text-neutral-100">
                      <div>{emp.name}</div>
                      <div className="text-[10px] text-neutral-400 font-normal">{emp.position}</div>
                    </td>
                    {daysOfWeek.map((day, dIndex) => {
                      const isWeekend = dIndex >= 5;
                      const isOnLeave = emp.status === 'ON_LEAVE' && !isWeekend;

                      return (
                        <td key={day} className="px-3 py-3 text-center">
                          {isWeekend ? (
                            <span className="text-[10px] text-neutral-300 dark:text-neutral-700">Off</span>
                          ) : isOnLeave ? (
                            <span className="inline-block px-2 py-1 rounded bg-amber-50 text-amber-700 dark:bg-amber-950/60 dark:text-amber-300 text-[10px] font-medium">
                              PTO
                            </span>
                          ) : (
                            <span className="inline-block px-2 py-1 rounded bg-neutral-100 text-neutral-800 dark:bg-[#1f1f1f] dark:text-neutral-200 text-[10px] font-medium">
                              09:00 - 17:30
                            </span>
                          )}
                        </td>
                      );
                    })}
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
