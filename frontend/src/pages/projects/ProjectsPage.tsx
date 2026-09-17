import React, { useState } from 'react';
import { FolderKanban, Plus, Search } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent } from '../../components/common/Card';
import { MOCK_PROJECTS } from '../../mocks/mockHrmData';
import { formatDate } from '../../utils/formatters';
import { useToast } from '../../context/ToastContext';

export const ProjectsPage: React.FC = () => {
  const { showToast } = useToast();
  const [search, setSearch] = useState('');

  const filtered = MOCK_PROJECTS.filter(
    (p) =>
      p.name.toLowerCase().includes(search.toLowerCase()) ||
      p.client.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title="Enterprise Projects"
        description="Active customer implementations, internal infrastructure sprints, and project milestones."
        actions={
          <Button
            size="sm"
            onClick={() => showToast('info', 'New Project', 'Create project modal dialog')}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            New Project
          </Button>
        }
      />

      <Card>
        <div className="p-4 border-b border-neutral-100 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414]">
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
            <input
              type="text"
              placeholder="Search projects by title or client..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full rounded-lg border border-neutral-200 bg-white pl-9 pr-4 py-2 text-xs text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100 dark:placeholder-neutral-500 dark:focus:border-white"
            />
          </div>
        </div>

        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
              <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                <tr>
                  <th className="px-5 py-3">Project Name</th>
                  <th className="px-5 py-3">Client / Department</th>
                  <th className="px-5 py-3">Progress</th>
                  <th className="px-5 py-3">Due Date</th>
                  <th className="px-5 py-3">Budget</th>
                  <th className="px-5 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {filtered.map((proj) => (
                  <tr
                    key={proj.id}
                    className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                  >
                    <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100 flex items-center gap-2">
                      <FolderKanban className="w-4 h-4 text-neutral-900 dark:text-neutral-100 shrink-0" />
                      <span>{proj.name}</span>
                    </td>
                    <td className="px-5 py-3.5">{proj.client}</td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2">
                        <div className="w-24 bg-neutral-100 dark:bg-[#1f1f1f] h-2 rounded-full overflow-hidden">
                          <div
                            className="bg-neutral-950 dark:bg-white h-full rounded-full"
                            style={{ width: `${proj.progress}%` }}
                          />
                        </div>
                        <span className="font-semibold text-[11px] text-neutral-800 dark:text-neutral-200">{proj.progress}%</span>
                      </div>
                    </td>
                    <td className="px-5 py-3.5">{formatDate(proj.dueDate)}</td>
                    <td className="px-5 py-3.5 font-mono font-medium text-zinc-900 dark:text-neutral-100">
                      {proj.budget}
                    </td>
                    <td className="px-5 py-3.5">
                      <Badge
                        variant={
                          proj.status === 'Delivered'
                            ? 'success'
                            : proj.status === 'On Track'
                            ? 'default'
                            : 'primary'
                        }
                        size="sm"
                        withDot
                      >
                        {proj.status}
                      </Badge>
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
