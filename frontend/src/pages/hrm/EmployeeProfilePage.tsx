import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Award,
  Building,
  Calendar,
  CheckCircle,
  Clock,
  FileText,
  Mail,
  MapPin,
  MessageSquare,
  Phone,
  Plus,
  Users,
} from 'lucide-react';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { Tabs } from '../../components/common/Tabs';
import { StatCard } from '../../components/widgets/StatCard';
import { INITIAL_EMPLOYEES } from '../../mocks/mockHrmData';
import { hrmApi } from '../../api/hrmApi';
import type { Employee } from '../../api/types';
import { useToast } from '../../context/ToastContext';
import { formatDate } from '../../utils/formatters';

export const EmployeeProfilePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { showToast } = useToast();

  const [activeTab, setActiveTab] = useState('overview');
  const [employee, setEmployee] = useState<Employee>(() => {
    return INITIAL_EMPLOYEES.find((e) => e.id === id) || INITIAL_EMPLOYEES[0];
  });

  useEffect(() => {
    if (!id) return;
    let isMounted = true;
    hrmApi.getEmployee(id)
      .then((data) => {
        if (isMounted && data) {
          setEmployee(data);
        }
      })
      .catch(() => {
        // Fallback to local mock if UUID not in tenant db (e.g. static mock ID)
        const fallback = INITIAL_EMPLOYEES.find((e) => e.id === id);
        if (fallback && isMounted) {
          setEmployee(fallback);
        }
      });
    return () => {
      isMounted = false;
    };
  }, [id]);

  const profileTabs = [
    { id: 'overview', label: 'Overview & Profile' },
    { id: 'attendance', label: 'Time & Attendance Logs' },
    { id: 'schedules', label: 'Work Shifts & Rota' },
    { id: 'claims', label: 'Expense Claims' },
    { id: 'documents', label: 'Compliance & Documents' },
  ];

  const handleAction = (actionName: string) => {
    showToast('info', actionName, `Action triggered for ${employee.name}`);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <Link
          to="/app/hrm/employees"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-zinc-500 hover:text-zinc-900 dark:hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Employee Directory</span>
        </Link>
      </div>

      {/* Profile Header Card */}
      <Card className="p-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
          <div className="flex items-start gap-4 sm:gap-5">
            <div
              className="flex h-16 w-16 sm:h-20 sm:w-20 items-center justify-center rounded-2xl bg-zinc-950 text-white dark:bg-white dark:text-neutral-950 font-bold text-2xl shadow-md shrink-0"
            >
              {employee.name.charAt(0)}
            </div>

            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-xl sm:text-2xl font-bold text-zinc-900 dark:text-neutral-100">
                  {employee.name}
                </h2>
                <Badge
                  variant={
                    employee.status === 'ACTIVE'
                      ? 'success'
                      : employee.status === 'ON_LEAVE'
                      ? 'warning'
                      : 'default'
                  }
                  size="sm"
                  withDot
                >
                  {employee.status.replace('_', ' ')}
                </Badge>
                <span className="text-xs font-mono text-neutral-500 bg-neutral-100 dark:bg-[#1f1f1f] px-2 py-0.5 rounded">
                  {employee.employeeId}
                </span>
              </div>

              <p className="text-sm font-medium text-neutral-600 dark:text-neutral-400">
                {employee.position} • {employee.department}
              </p>

              <div className="flex flex-wrap items-center gap-4 text-xs text-neutral-500 dark:text-neutral-400 pt-1">
                <span className="flex items-center gap-1">
                  <Mail className="w-3.5 h-3.5" />
                  {employee.email}
                </span>
                <span className="flex items-center gap-1">
                  <Phone className="w-3.5 h-3.5" />
                  {employee.phone}
                </span>
                <span className="flex items-center gap-1">
                  <MapPin className="w-3.5 h-3.5" />
                  {employee.location}
                </span>
                <span className="flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5" />
                  Hired {formatDate(employee.hireDate)}
                </span>
              </div>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="flex items-center gap-2.5 self-start md:self-auto shrink-0">
            <Button
              variant="outline"
              size="sm"
              leftIcon={<MessageSquare className="w-4 h-4" />}
              onClick={() => handleAction('Send Direct Message')}
            >
              Message
            </Button>
            <Button
              size="sm"
              leftIcon={<Plus className="w-4 h-4" />}
              onClick={() => handleAction('Assign New Task')}
            >
              Assign Task
            </Button>
          </div>
        </div>

        {/* Tabs */}
        <div className="mt-8">
          <Tabs tabs={profileTabs} activeTab={activeTab} onChange={setActiveTab} />
        </div>
      </Card>

      {/* Tab Content */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          {/* Quick Stats */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <StatCard
              title="Attendance Rate"
              value={`${employee.attendanceRate}%`}
              change={{ value: '+1.2%', isPositive: true, periodText: 'vs avg' }}
              icon={<CheckCircle className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />}
              iconBgColor="bg-emerald-50 dark:bg-emerald-950/40"
            />
            <StatCard
              title="Hours Logged (Mo)"
              value={`${employee.billableHours}h`}
              change={{ value: '100% billable', isPositive: true }}
              icon={<Clock className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
              iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
            />
            <StatCard
              title="Work Model"
              value={employee.workModel}
              icon={<Building className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
              iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
            />
            <StatCard
              title="Manager / Lead"
              value={employee.manager || 'Unassigned'}
              icon={<Users className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
              iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
            />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Employment Details */}
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>Employment & Contract Details</CardTitle>
              </CardHeader>
              <CardContent>
                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4 text-xs">
                  <div>
                    <dt className="text-zinc-400 font-medium uppercase tracking-wider text-[10px]">
                      Official Job Title
                    </dt>
                    <dd className="font-semibold text-zinc-800 dark:text-neutral-200 mt-1">
                      {employee.position}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-zinc-400 font-medium uppercase tracking-wider text-[10px]">
                      Assigned Department
                    </dt>
                    <dd className="font-semibold text-zinc-800 dark:text-neutral-200 mt-1">
                      {employee.department}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-zinc-400 font-medium uppercase tracking-wider text-[10px]">
                      Work Schedule Model
                    </dt>
                    <dd className="font-semibold text-zinc-800 dark:text-neutral-200 mt-1">
                      Standard Full-Time (40h Mon-Fri)
                    </dd>
                  </div>
                  <div>
                    <dt className="text-zinc-400 font-medium uppercase tracking-wider text-[10px]">
                      Direct Supervisor
                    </dt>
                    <dd className="font-semibold text-zinc-800 dark:text-neutral-200 mt-1">
                      {employee.manager}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-zinc-400 font-medium uppercase tracking-wider text-[10px]">
                      Contract Type
                    </dt>
                    <dd className="font-semibold text-zinc-800 dark:text-neutral-200 mt-1">
                      Full-Time Permanent
                    </dd>
                  </div>
                  <div>
                    <dt className="text-zinc-400 font-medium uppercase tracking-wider text-[10px]">
                      Primary Worksite
                    </dt>
                    <dd className="font-semibold text-zinc-800 dark:text-neutral-200 mt-1">
                      {employee.location} ({employee.workModel})
                    </dd>
                  </div>
                </dl>
              </CardContent>
            </Card>

            {/* Skills & Badges */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Award className="w-4 h-4 text-zinc-900 dark:text-neutral-100" />
                  <span>Skills & Core Competencies</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex flex-wrap gap-1.5">
                  <Badge variant="primary" size="sm">System Architecture</Badge>
                  <Badge variant="default" size="sm">TypeScript</Badge>
                  <Badge variant="info" size="sm">Spring Boot</Badge>
                  <Badge variant="success" size="sm">PostgreSQL</Badge>
                  <Badge variant="default" size="sm">Team Leadership</Badge>
                  <Badge variant="default" size="sm">OAuth / OIDC</Badge>
                </div>

                <div className="pt-4 border-t border-neutral-100 dark:border-[#262626] space-y-2">
                  <p className="text-xs font-semibold text-neutral-700 dark:text-neutral-300">
                    Security & Compliance
                  </p>
                  <p className="text-[11px] text-neutral-500">
                    SOC2 Security Awareness (Completed Sep 2026)
                  </p>
                  <p className="text-[11px] text-neutral-500">
                    GDPR Data Protection Principles (Certified)
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {activeTab !== 'overview' && (
        <Card className="p-12 text-center text-zinc-500 space-y-2">
          <FileText className="w-8 h-8 mx-auto text-zinc-400" />
          <h4 className="text-sm font-semibold text-zinc-800 dark:text-neutral-200">
            {profileTabs.find((t) => t.id === activeTab)?.label}
          </h4>
          <p className="text-xs max-w-md mx-auto">
            Operational records and documentation for {employee.name} are maintained in the central HRM vault.
          </p>
        </Card>
      )}
    </div>
  );
};
