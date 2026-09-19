import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  Calendar,
  ChevronRight,
  Filter,
  Grid,
  List as ListIcon,
  Mail,
  Phone,
  Plus,
  RefreshCw,
  Search,
  UserCheck,
  Users,
} from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card } from '../../components/common/Card';
import { Tabs } from '../../components/common/Tabs';
import { StatCard } from '../../components/widgets/StatCard';
import { EmptyState } from '../../components/common/EmptyState';
import { EmployeeInviteModal } from './EmployeeInviteModal';
import { INITIAL_EMPLOYEES } from '../../mocks/mockHrmData';
import { hrmApi } from '../../api/hrmApi';
import type { Employee } from '../../api/types';
import { useAuth } from '../../context/AuthContext';

export const EmployeeListPage: React.FC = () => {
  const { hasRole } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const [employees, setEmployees] = useState<Employee[]>(INITIAL_EMPLOYEES);
  const [isLoading, setIsLoading] = useState(false);
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState(searchParams.get('search') || '');
  const [selectedDept, setSelectedDept] = useState('ALL');
  const [activeTab, setActiveTab] = useState('ALL');
  const [viewMode, setViewMode] = useState<'table' | 'grid'>('table');

  const handleRefresh = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await hrmApi.getEmployees();
      if (Array.isArray(data)) {
        setEmployees(data);
      }
    } catch {
      // Fallback to initial seed if backend not reachable in preview
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;
    hrmApi.getEmployees()
      .then((data) => {
        if (isMounted && Array.isArray(data)) {
          setEmployees(data);
        }
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });
    return () => {
      isMounted = false;
    };
  }, []);

  const statusTabs = [
    { id: 'ALL', label: 'All Staff', count: employees.length },
    {
      id: 'ACTIVE',
      label: 'Active',
      count: employees.filter((e) => e.status === 'ACTIVE').length,
    },
    {
      id: 'ON_LEAVE',
      label: 'On Leave',
      count: employees.filter((e) => e.status === 'ON_LEAVE').length,
    },
    {
      id: 'PROBATION',
      label: 'Probation',
      count: employees.filter((e) => e.status === 'PROBATION').length,
    },
    {
      id: 'INACTIVE',
      label: 'Inactive',
      count: employees.filter((e) => e.status === 'INACTIVE' || e.status === 'TERMINATED').length,
    },
  ];

  const departments = useMemo(() => {
    const set = new Set(employees.map((e) => e.department));
    return ['ALL', ...Array.from(set)];
  }, [employees]);

  const filteredEmployees = useMemo(() => {
    return employees.filter((emp) => {
      const matchesSearch =
        searchQuery.trim() === '' ||
        emp.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        emp.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
        emp.employeeId.toLowerCase().includes(searchQuery.toLowerCase()) ||
        emp.position.toLowerCase().includes(searchQuery.toLowerCase());

      const matchesDept = selectedDept === 'ALL' || emp.department === selectedDept;

      const matchesStatus = activeTab === 'ALL' || emp.status === activeTab;

      return matchesSearch && matchesDept && matchesStatus;
    });
  }, [employees, searchQuery, selectedDept, activeTab]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Employee Directory"
        description="Manage organizational staff, view employment status, and invite team members."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleRefresh}
              isLoading={isLoading}
              leftIcon={<RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />}
            >
              Refresh
            </Button>
            {hasRole('ADMIN') && (
              <Button
                onClick={() => setIsInviteModalOpen(true)}
                leftIcon={<Plus className="w-4 h-4" />}
              >
                Invite Teammate
              </Button>
            )}
          </div>
        }
      />

      {/* KPI Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Staff"
          value={employees.length}
          icon={<Users className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
          iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
        />
        <StatCard
          title="Active Workforce"
          value={employees.filter((e) => e.status === 'ACTIVE').length}
          icon={<UserCheck className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />}
          iconBgColor="bg-emerald-50 dark:bg-emerald-950/40"
        />
        <StatCard
          title="On Approved Leave"
          value={employees.filter((e) => e.status === 'ON_LEAVE').length}
          icon={<Calendar className="w-5 h-5 text-amber-600 dark:text-amber-400" />}
          iconBgColor="bg-amber-50 dark:bg-amber-950/40"
        />
        <StatCard
          title="Probation Period"
          value={employees.filter((e) => e.status === 'PROBATION').length}
          icon={<Users className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
          iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
        />
      </div>

      {/* Tabs & Controls */}
      <Card>
        <div className="p-4 border-b border-neutral-100 dark:border-[#262626] flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <Tabs tabs={statusTabs} activeTab={activeTab} onChange={setActiveTab} />

          <div className="flex items-center gap-1 self-end sm:self-auto shrink-0">
            <button
              type="button"
              onClick={() => setViewMode('table')}
              className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
                viewMode === 'table'
                  ? 'bg-neutral-100 text-neutral-950 dark:bg-[#1f1f1f] dark:text-white'
                  : 'text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-200'
              }`}
              aria-label="Table view"
            >
              <ListIcon className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={() => setViewMode('grid')}
              className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
                viewMode === 'grid'
                  ? 'bg-neutral-100 text-neutral-950 dark:bg-[#1f1f1f] dark:text-white'
                  : 'text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-200'
              }`}
              aria-label="Grid view"
            >
              <Grid className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Filter Toolbar */}
        <div className="p-4 border-b border-neutral-100 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414] flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
            <input
              type="text"
              placeholder="Search by name, ID, position, or email..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setSearchParams(e.target.value ? { search: e.target.value } : {});
              }}
              className="w-full rounded-lg border border-neutral-200 bg-white pl-9 pr-4 py-2 text-xs text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100 dark:placeholder-neutral-500 dark:focus:border-white"
            />
          </div>

          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-neutral-400 shrink-0" />
            <select
              value={selectedDept}
              onChange={(e) => setSelectedDept(e.target.value)}
              className="rounded-lg border border-neutral-200 bg-white px-3 py-2 text-xs text-neutral-700 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900 dark:focus:border-white cursor-pointer"
            >
              {departments.map((dept) => (
                <option key={dept} value={dept}>
                  {dept === 'ALL' ? 'All Departments' : dept}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Employee View */}
        {filteredEmployees.length === 0 ? (
          <EmptyState
            icon={<Users className="w-6 h-6" />}
            title="No staff found matching criteria"
            description="Try clearing your search query or department filter to see all employees."
            actionLabel="Reset Filters"
            onAction={() => {
              setSearchQuery('');
              setSelectedDept('ALL');
              setActiveTab('ALL');
            }}
            className="border-0 rounded-none bg-transparent"
          />
        ) : viewMode === 'table' ? (
          /* Table View */
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
              <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                <tr>
                  <th className="px-5 py-3.5">Employee</th>
                  <th className="px-5 py-3.5">Role / Position</th>
                  <th className="px-5 py-3.5">Department</th>
                  <th className="px-5 py-3.5">Schedule Model</th>
                  <th className="px-5 py-3.5">Status</th>
                  <th className="px-5 py-3.5">Joined Date</th>
                  <th className="px-5 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {filteredEmployees.map((emp) => (
                  <tr
                    key={emp.id}
                    className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors group"
                  >
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <div
                          className="flex h-8 w-8 items-center justify-center rounded-full bg-neutral-900 text-white dark:bg-white dark:text-[#0a0a0a] font-bold text-xs shrink-0"
                        >
                          {emp.name.charAt(0)}
                        </div>
                        <div>
                          <Link
                            to={`/app/hrm/employees/${emp.id}`}
                            className="font-semibold text-neutral-900 hover:underline dark:text-neutral-100 transition-colors"
                          >
                            {emp.name}
                          </Link>
                          <p className="text-[11px] text-neutral-400">{emp.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 font-medium text-neutral-800 dark:text-neutral-200">
                      {emp.position}
                    </td>
                    <td className="px-5 py-3.5">{emp.department}</td>
                    <td className="px-5 py-3.5 font-mono text-[11px]">{emp.workModel}</td>
                    <td className="px-5 py-3.5">
                      <Badge
                        variant={
                          emp.status === 'ACTIVE'
                            ? 'success'
                            : emp.status === 'ON_LEAVE'
                            ? 'warning'
                            : emp.status === 'PROBATION'
                            ? 'info'
                            : 'default'
                        }
                        size="sm"
                        withDot
                      >
                        {emp.status.replace('_', ' ')}
                      </Badge>
                    </td>
                    <td className="px-5 py-3.5 text-neutral-400">{emp.hireDate}</td>
                    <td className="px-5 py-3.5 text-right">
                      <Link
                        to={`/app/hrm/employees/${emp.id}`}
                        className="inline-flex items-center gap-1 text-xs font-semibold text-neutral-900 hover:underline dark:text-neutral-100"
                      >
                        Profile <ChevronRight className="w-3.5 h-3.5" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          /* Grid View */
          <div className="p-5 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredEmployees.map((emp) => (
              <div
                key={emp.id}
                className="p-4 rounded-xl border border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#141414] hover:border-neutral-400 dark:hover:border-neutral-600 transition-all flex flex-col justify-between space-y-4"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-3">
                    <div
                      className="flex h-10 w-10 items-center justify-center rounded-full bg-neutral-900 text-white dark:bg-white dark:text-[#0a0a0a] font-bold text-xs shrink-0"
                    >
                      {emp.name.charAt(0)}
                    </div>
                    <div>
                      <Link
                        to={`/app/hrm/employees/${emp.id}`}
                        className="font-semibold text-xs text-neutral-900 dark:text-neutral-100 hover:underline block"
                      >
                        {emp.name}
                      </Link>
                      <span className="text-[10px] text-neutral-400 font-mono">
                        {emp.employeeId}
                      </span>
                    </div>
                  </div>
                  <Badge
                    variant={
                      emp.status === 'ACTIVE'
                        ? 'success'
                        : emp.status === 'ON_LEAVE'
                        ? 'warning'
                        : emp.status === 'PROBATION'
                        ? 'info'
                        : 'default'
                    }
                    size="sm"
                  >
                    {emp.status.replace('_', ' ')}
                  </Badge>
                </div>

                <div className="space-y-1.5 text-xs text-neutral-600 dark:text-neutral-400 border-t border-neutral-100 dark:border-[#262626] pt-3">
                  <p className="font-medium text-neutral-900 dark:text-neutral-200">
                    {emp.position}
                  </p>
                  <p className="text-[11px] text-neutral-500">{emp.department}</p>
                  <div className="flex items-center gap-2 text-[11px] pt-1">
                    <Mail className="w-3 h-3 text-neutral-400" />
                    <span className="truncate">{emp.email}</span>
                  </div>
                  <div className="flex items-center gap-2 text-[11px]">
                    <Phone className="w-3 h-3 text-neutral-400" />
                    <span>{emp.phone}</span>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-neutral-100 dark:border-[#262626]">
                  <span className="text-[11px] text-neutral-400">{emp.workModel}</span>
                  <Link to={`/app/hrm/employees/${emp.id}`}>
                    <Button variant="outline" size="sm">
                      View Profile
                    </Button>
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* Teammate Invite Modal */}
      <EmployeeInviteModal
        isOpen={isInviteModalOpen}
        onClose={() => setIsInviteModalOpen(false)}
        onSuccess={handleRefresh}
      />
    </div>
  );
};
