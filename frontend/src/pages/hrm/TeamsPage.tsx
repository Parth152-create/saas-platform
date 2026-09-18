import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Building2, Plus, RefreshCw } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card } from '../../components/common/Card';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { MOCK_DEPARTMENTS } from '../../mocks/mockHrmData';
import { hrmApi } from '../../api/hrmApi';
import type { DepartmentSummary } from '../../api/types';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';

export const TeamsPage: React.FC = () => {
  const { hasRole } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [departments, setDepartments] = useState<DepartmentSummary[]>(MOCK_DEPARTMENTS);
  const [isLoading, setIsLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // New department form state
  const [name, setName] = useState('');
  const [lead, setLead] = useState('');
  const [budgetUtilization, setBudgetUtilization] = useState('50');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleRefresh = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await hrmApi.getDepartments();
      if (data && data.length > 0) {
        setDepartments(data);
      }
    } catch {
      // Fallback to local default if backend unreachable
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;
    hrmApi.getDepartments()
      .then((data) => {
        if (isMounted && data && data.length > 0) {
          setDepartments(data);
        }
      })
      .catch(() => {});
    return () => {
      isMounted = false;
    };
  }, []);

  const handleCreateDepartment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Department name is required');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      await hrmApi.createDepartment({
        name: name.trim(),
        lead: lead.trim() || undefined,
        budgetUtilization: budgetUtilization ? parseFloat(budgetUtilization) : 0,
      });

      showToast('success', 'Department Created', `${name.trim()} added to workforce`);
      setName('');
      setLead('');
      setBudgetUtilization('50');
      setIsModalOpen(false);
      handleRefresh();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to create department';
      setError(msg);
      showToast('error', 'Creation Failed', msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Teams & Departments"
        description="Workforce organization hierarchies, leadership assignments, and department budgets."
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
                size="sm"
                onClick={() => setIsModalOpen(true)}
                leftIcon={<Plus className="w-4 h-4" />}
              >
                Create Department
              </Button>
            )}
          </div>
        }
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {departments.map((dept) => (
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
                Department Lead: <strong className="text-neutral-800 dark:text-neutral-200">{dept.lead || 'Unassigned'}</strong>
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
                  style={{ width: `${Math.min(dept.budgetUtilization, 100)}%` }}
                />
              </div>
            </div>

            <Button
              variant="outline"
              size="sm"
              className="w-full"
              onClick={() => navigate(`/app/hrm/employees?department=${encodeURIComponent(dept.name)}`)}
            >
              View Team Members
            </Button>
          </Card>
        ))}
      </div>

      {/* Create Department Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create New Department"
        description="Establish a new department within your workspace organization."
      >
        <form onSubmit={handleCreateDepartment} className="space-y-4">
          {error && (
            <div className="p-3 rounded-lg bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300 text-xs">
              {error}
            </div>
          )}

          <Input
            label="Department Name"
            placeholder="e.g. Operations, Legal, Infrastructure"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Input
            label="Department Lead"
            placeholder="e.g. Sarah Jenkins"
            value={lead}
            onChange={(e) => setLead(e.target.value)}
          />

          <Input
            label="Budget Allocation / Utilization (%)"
            type="number"
            min="0"
            max="100"
            value={budgetUtilization}
            onChange={(e) => setBudgetUtilization(e.target.value)}
          />

          <div className="flex justify-end gap-2 pt-3 border-t border-neutral-100 dark:border-[#262626]">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setIsModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              size="sm"
              isLoading={isSubmitting}
            >
              Create Department
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
