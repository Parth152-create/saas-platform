import React, { useEffect, useState, useCallback } from 'react';
import {
  Calendar,
  CheckCircle2,
  Filter,
  Plus,
  RefreshCw,
  UserCheck,
  XCircle,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent } from '../../components/common/Card';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Tabs } from '../../components/common/Tabs';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { leaveApi } from '../../api/leaveApi';
import type {
  CreateLeaveRequest,
  LeaveBalance,
  LeaveRequest,
  LeaveStatus,
  LeaveType,
} from '../../api/types';
import { formatDate } from '../../utils/formatters';

export const LeaveManagementPage: React.FC = () => {
  const { hasRole, user } = useAuth();
  const { showToast } = useToast();

  const isManagerOrAdmin = hasRole('MANAGER') || hasRole('ADMIN') || hasRole('SUPER_ADMIN');

  const [activeTab, setActiveTab] = useState<'my_requests' | 'approval_queue'>('my_requests');
  const [balances, setBalances] = useState<LeaveBalance[]>([]);
  const [myRequests, setMyRequests] = useState<LeaveRequest[]>([]);
  const [allRequests, setAllRequests] = useState<LeaveRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Filters
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');

  // Request Modal state
  const [isRequestModalOpen, setIsRequestModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [requestForm, setRequestForm] = useState<CreateLeaveRequest>({
    leaveType: 'ANNUAL',
    startDate: '',
    endDate: '',
    reason: '',
  });

  // Review Modal state
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<LeaveRequest | null>(null);
  const [reviewNote, setReviewNote] = useState('');
  const [isReviewing, setIsReviewing] = useState(false);

  const openRequestModal = () => {
    const today = new Date().toISOString().split('T')[0];
    const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
    setRequestForm({
      leaveType: 'ANNUAL',
      startDate: today,
      endDate: tomorrow,
      reason: '',
    });
    setIsRequestModalOpen(true);
  };

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const promises: [Promise<LeaveBalance[]>, Promise<LeaveRequest[]>, Promise<LeaveRequest[]>?] = [
        leaveApi.getMyLeaveBalances(),
        leaveApi.getMyLeaveRequests(),
      ];

      if (isManagerOrAdmin) {
        promises.push(
          leaveApi.getAllLeaveRequests({
            status: statusFilter !== 'ALL' ? (statusFilter as LeaveStatus) : undefined,
            type: typeFilter !== 'ALL' ? (typeFilter as LeaveType) : undefined,
          })
        );
      }

      const results = await Promise.all(promises);
      setBalances(results[0]);
      setMyRequests(results[1]);
      if (results[2]) {
        setAllRequests(results[2]);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load leave data';
      showToast('error', 'Error', msg);
    } finally {
      setIsLoading(false);
    }
  }, [isManagerOrAdmin, statusFilter, typeFilter, showToast]);

  useEffect(() => {
    let isMounted = true;
    const promises: [Promise<LeaveBalance[]>, Promise<LeaveRequest[]>, Promise<LeaveRequest[]>?] = [
      leaveApi.getMyLeaveBalances(),
      leaveApi.getMyLeaveRequests(),
    ];

    if (isManagerOrAdmin) {
      promises.push(
        leaveApi.getAllLeaveRequests({
          status: statusFilter !== 'ALL' ? (statusFilter as LeaveStatus) : undefined,
          type: typeFilter !== 'ALL' ? (typeFilter as LeaveType) : undefined,
        })
      );
    }

    Promise.all(promises)
      .then((results) => {
        if (isMounted) {
          setBalances(results[0]);
          setMyRequests(results[1]);
          if (results[2]) {
            setAllRequests(results[2]);
          }
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg = err instanceof Error ? err.message : 'Failed to load leave data';
          showToast('error', 'Error', msg);
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isManagerOrAdmin, statusFilter, typeFilter, showToast]);

  const handleCreateRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!requestForm.reason.trim()) {
      showToast('warning', 'Missing Reason', 'Please provide a reason for the leave request.');
      return;
    }

    try {
      setIsSubmitting(true);
      await leaveApi.createLeaveRequest(requestForm);
      showToast('success', 'Leave Requested', 'Your leave request has been submitted for manager approval.');
      setIsRequestModalOpen(false);
      setRequestForm({
        leaveType: 'ANNUAL',
        startDate: '',
        endDate: '',
        reason: '',
      });
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to submit leave request';
      showToast('error', 'Submission Failed', msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancelRequest = async (id: string) => {
    if (!window.confirm('Are you sure you want to cancel this leave request?')) return;
    try {
      await leaveApi.cancelLeaveRequest(id);
      showToast('info', 'Request Cancelled', 'Leave request cancelled.');
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to cancel request';
      showToast('error', 'Cancellation Failed', msg);
    }
  };

  const handleApprove = async () => {
    if (!selectedRequest) return;
    try {
      setIsReviewing(true);
      await leaveApi.approveLeaveRequest(selectedRequest.id, { reviewNote });
      showToast('success', 'Leave Approved', `Leave request for ${selectedRequest.employeeName} approved.`);
      setIsReviewModalOpen(false);
      setSelectedRequest(null);
      setReviewNote('');
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to approve request';
      showToast('error', 'Approval Error', msg);
    } finally {
      setIsReviewing(false);
    }
  };

  const handleReject = async () => {
    if (!selectedRequest) return;
    try {
      setIsReviewing(true);
      await leaveApi.rejectLeaveRequest(selectedRequest.id, { reviewNote });
      showToast('info', 'Leave Rejected', `Leave request for ${selectedRequest.employeeName} rejected.`);
      setIsReviewModalOpen(false);
      setSelectedRequest(null);
      setReviewNote('');
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to reject request';
      showToast('error', 'Rejection Error', msg);
    } finally {
      setIsReviewing(false);
    }
  };

  const openReviewModal = (req: LeaveRequest) => {
    setSelectedRequest(req);
    setReviewNote('');
    setIsReviewModalOpen(true);
  };

  const pendingApprovalCount = allRequests.filter((r) => r.status === 'PENDING').length;

  const tabs = [
    { id: 'my_requests', label: 'My Leave Requests', count: myRequests.length },
    ...(isManagerOrAdmin
      ? [{ id: 'approval_queue', label: 'Team Approval Queue', count: pendingApprovalCount }]
      : []),
  ];

  const getStatusBadge = (status: LeaveStatus) => {
    switch (status) {
      case 'APPROVED':
        return <Badge variant="success" size="sm" withDot>Approved</Badge>;
      case 'PENDING':
        return <Badge variant="warning" size="sm" withDot>Pending Review</Badge>;
      case 'REJECTED':
        return <Badge variant="danger" size="sm" withDot>Rejected</Badge>;
      case 'CANCELLED':
        return <Badge variant="default" size="sm" withDot>Cancelled</Badge>;
      default:
        return <Badge size="sm">{status}</Badge>;
    }
  };

  const filteredMyRequests = myRequests.filter((r) => {
    if (statusFilter !== 'ALL' && r.status !== statusFilter) return false;
    if (typeFilter !== 'ALL' && r.leaveType !== typeFilter) return false;
    return true;
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Leave &amp; Absence Management"
        description="Submit leave requests, track remaining allowances, and manage team approval workflows."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={loadData}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              Sync
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={openRequestModal}
              leftIcon={<Plus className="w-3.5 h-3.5" />}
            >
              Request Leave
            </Button>
          </div>
        }
      />

      {/* Balances summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {isLoading && balances.length === 0 ? (
          Array.from({ length: 4 }).map((_, i) => (
            <LoadingSkeleton key={i} className="h-28 rounded-xl" />
          ))
        ) : (
          balances.map((b) => (
            <Card key={b.id} className="p-4 flex flex-col justify-between">
              <div>
                <span className="text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:text-neutral-500">
                  {b.leaveType.replace('_', ' ')}
                </span>
                <div className="flex items-baseline gap-1 mt-1.5">
                  <span className="text-3xl font-bold text-neutral-900 dark:text-neutral-100">
                    {b.remainingDays}
                  </span>
                  <span className="text-xs text-neutral-500">days remaining</span>
                </div>
              </div>
              <div className="mt-3 pt-2.5 border-t border-neutral-100 dark:border-[#262626] flex items-center justify-between text-[11px] text-neutral-500">
                <span>{b.usedDays} days used</span>
                <span>{b.totalDays} days total</span>
              </div>
            </Card>
          ))
        )}
      </div>

      {/* Tabs and Filters */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-neutral-200 dark:border-[#262626] pb-1">
        <Tabs
          tabs={tabs}
          activeTab={activeTab}
          onChange={(tab) => setActiveTab(tab as 'my_requests' | 'approval_queue')}
        />

        <div className="flex items-center gap-2">
          <Filter className="w-3.5 h-3.5 text-neutral-400" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="text-xs rounded-lg border border-neutral-200 bg-white dark:bg-[#141414] dark:border-[#262626] px-2.5 py-1 text-neutral-700 dark:text-neutral-300"
          >
            <option value="ALL">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="CANCELLED">Cancelled</option>
          </select>

          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="text-xs rounded-lg border border-neutral-200 bg-white dark:bg-[#141414] dark:border-[#262626] px-2.5 py-1 text-neutral-700 dark:text-neutral-300"
          >
            <option value="ALL">All Leave Types</option>
            <option value="ANNUAL">Annual Leave</option>
            <option value="SICK">Sick Leave</option>
            <option value="CASUAL">Casual / Personal</option>
            <option value="UNPAID">Unpaid Leave</option>
            <option value="PARENTAL">Parental Leave</option>
            <option value="BEREAVEMENT">Bereavement Leave</option>
          </select>
        </div>
      </div>

      {/* Tab 1: My Requests */}
      {activeTab === 'my_requests' && (
        <Card>
          <CardContent className="p-0">
            {filteredMyRequests.length === 0 ? (
              <div className="p-12 text-center text-xs text-neutral-500">
                <Calendar className="w-8 h-8 mx-auto text-neutral-400 mb-3 opacity-50" />
                No leave requests found matching the current filters.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Leave Type</th>
                      <th className="px-5 py-3">Dates</th>
                      <th className="px-5 py-3">Days</th>
                      <th className="px-5 py-3">Status</th>
                      <th className="px-5 py-3">Reason</th>
                      <th className="px-5 py-3">Reviewer / Notes</th>
                      <th className="px-5 py-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {filteredMyRequests.map((req) => (
                      <tr key={req.id} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors">
                        <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                          {req.leaveType.replace('_', ' ')}
                        </td>
                        <td className="px-5 py-3.5 whitespace-nowrap">
                          {formatDate(req.startDate)} – {formatDate(req.endDate)}
                        </td>
                        <td className="px-5 py-3.5 font-bold text-neutral-900 dark:text-neutral-100">
                          {req.daysCount}d
                        </td>
                        <td className="px-5 py-3.5">
                          {getStatusBadge(req.status)}
                        </td>
                        <td className="px-5 py-3.5 max-w-xs truncate text-neutral-500">
                          {req.reason}
                        </td>
                        <td className="px-5 py-3.5 text-[11px]">
                          {req.reviewerName ? (
                            <div>
                              <span className="font-semibold text-neutral-800 dark:text-neutral-200">
                                {req.reviewerName}
                              </span>
                              {req.reviewNote && (
                                <p className="text-neutral-400 italic mt-0.5">&quot;{req.reviewNote}&quot;</p>
                              )}
                            </div>
                          ) : (
                            <span className="text-neutral-400">—</span>
                          )}
                        </td>
                        <td className="px-5 py-3.5 text-right whitespace-nowrap">
                          {req.status === 'PENDING' && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleCancelRequest(req.id)}
                              className="text-neutral-500 hover:text-red-600 hover:border-red-300"
                            >
                              Cancel
                            </Button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Tab 2: Approval Queue (Managers & Admins) */}
      {activeTab === 'approval_queue' && isManagerOrAdmin && (
        <Card>
          <CardContent className="p-0">
            {allRequests.length === 0 ? (
              <div className="p-12 text-center text-xs text-neutral-500">
                <UserCheck className="w-8 h-8 mx-auto text-neutral-400 mb-3 opacity-50" />
                No team leave requests awaiting your action.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Employee</th>
                      <th className="px-5 py-3">Leave Type</th>
                      <th className="px-5 py-3">Period</th>
                      <th className="px-5 py-3">Days</th>
                      <th className="px-5 py-3">Status</th>
                      <th className="px-5 py-3">Reason</th>
                      <th className="px-5 py-3 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {allRequests.map((req) => {
                      const isOwnRequest = req.employeeEmail === user?.email;
                      return (
                        <tr key={req.id} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors">
                          <td className="px-5 py-3.5">
                            <div className="font-semibold text-neutral-900 dark:text-neutral-100">
                              {req.employeeName}
                            </div>
                            <div className="text-[11px] text-neutral-400">
                              {req.employeeEmail}
                            </div>
                          </td>
                          <td className="px-5 py-3.5 font-medium text-neutral-800 dark:text-neutral-200">
                            {req.leaveType.replace('_', ' ')}
                          </td>
                          <td className="px-5 py-3.5 whitespace-nowrap">
                            {formatDate(req.startDate)} – {formatDate(req.endDate)}
                          </td>
                          <td className="px-5 py-3.5 font-bold text-neutral-900 dark:text-neutral-100">
                            {req.daysCount}d
                          </td>
                          <td className="px-5 py-3.5">
                            {getStatusBadge(req.status)}
                          </td>
                          <td className="px-5 py-3.5 max-w-xs truncate text-neutral-500">
                            {req.reason}
                          </td>
                          <td className="px-5 py-3.5 text-right whitespace-nowrap">
                            {req.status === 'PENDING' ? (
                              isOwnRequest ? (
                                <span className="text-[11px] text-neutral-400 italic">Self-approval barred</span>
                              ) : (
                                <Button
                                  variant="primary"
                                  size="sm"
                                  onClick={() => openReviewModal(req)}
                                >
                                  Review
                                </Button>
                              )
                            ) : (
                              <span className="text-[11px] text-neutral-400">Reviewed</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Request Leave Modal */}
      <Modal
        isOpen={isRequestModalOpen}
        onClose={() => setIsRequestModalOpen(false)}
        title="Submit Leave Request"
        description="Select the absence type and date range. Upon submission, an approval notification will be dispatched to your team lead."
        footer={
          <>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsRequestModalOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleCreateRequest}
              isLoading={isSubmitting}
              leftIcon={<CheckCircle2 className="w-3.5 h-3.5" />}
            >
              Submit Request
            </Button>
          </>
        }
      >
        <form onSubmit={handleCreateRequest} className="space-y-4">
          <Select
            label="Leave Type"
            value={requestForm.leaveType}
            onChange={(e) =>
              setRequestForm((prev) => ({ ...prev, leaveType: e.target.value as LeaveType }))
            }
            options={[
              { value: 'ANNUAL', label: 'Annual Leave (PTO)' },
              { value: 'SICK', label: 'Sick Leave' },
              { value: 'CASUAL', label: 'Casual / Personal Leave' },
              { value: 'UNPAID', label: 'Unpaid Leave' },
              { value: 'PARENTAL', label: 'Parental Leave' },
              { value: 'BEREAVEMENT', label: 'Bereavement Leave' },
            ]}
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Start Date"
              type="date"
              value={requestForm.startDate}
              onChange={(e) =>
                setRequestForm((prev) => ({ ...prev, startDate: e.target.value }))
              }
              required
            />
            <Input
              label="End Date"
              type="date"
              value={requestForm.endDate}
              onChange={(e) =>
                setRequestForm((prev) => ({ ...prev, endDate: e.target.value }))
              }
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 dark:text-neutral-300 uppercase tracking-wider mb-1.5">
              Reason / Notes
            </label>
            <textarea
              rows={3}
              value={requestForm.reason}
              onChange={(e) =>
                setRequestForm((prev) => ({ ...prev, reason: e.target.value }))
              }
              placeholder="e.g., Annual family vacation or medical recovery..."
              className="w-full rounded-lg border border-neutral-300 dark:border-[#262626] bg-white dark:bg-[#141414] px-3 py-2 text-sm text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900"
              required
            />
          </div>
        </form>
      </Modal>

      {/* Review Request Modal */}
      <Modal
        isOpen={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        title="Review Leave Submission"
        description={`Take action on leave request from ${selectedRequest?.employeeName || 'employee'}.`}
        footer={
          <div className="flex items-center justify-between w-full">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsReviewModalOpen(false)}
              disabled={isReviewing}
            >
              Cancel
            </Button>
            <div className="flex items-center gap-2">
              <Button
                variant="danger"
                size="sm"
                onClick={handleReject}
                isLoading={isReviewing}
                leftIcon={<XCircle className="w-3.5 h-3.5" />}
              >
                Reject
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleApprove}
                isLoading={isReviewing}
                leftIcon={<CheckCircle2 className="w-3.5 h-3.5" />}
              >
                Approve
              </Button>
            </div>
          </div>
        }
      >
        {selectedRequest && (
          <div className="space-y-4">
            <div className="p-3.5 bg-neutral-50 dark:bg-[#1f1f1f] rounded-lg border border-neutral-200 dark:border-[#262626] space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-neutral-500">Employee:</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100">
                  {selectedRequest.employeeName} ({selectedRequest.employeeEmail})
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-500">Type:</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100">
                  {selectedRequest.leaveType.replace('_', ' ')}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-500">Duration:</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100">
                  {formatDate(selectedRequest.startDate)} – {formatDate(selectedRequest.endDate)} ({selectedRequest.daysCount} days)
                </span>
              </div>
              <div className="pt-2 border-t border-neutral-200 dark:border-[#262626]">
                <span className="text-neutral-500">Reason:</span>
                <p className="mt-1 text-neutral-800 dark:text-neutral-200 italic font-normal">
                  &quot;{selectedRequest.reason}&quot;
                </p>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 dark:text-neutral-300 uppercase tracking-wider mb-1.5">
                Reviewer Notes (Optional)
              </label>
              <textarea
                rows={2}
                value={reviewNote}
                onChange={(e) => setReviewNote(e.target.value)}
                placeholder="e.g., Approved. Please coordinate handoff with the team..."
                className="w-full rounded-lg border border-neutral-300 dark:border-[#262626] bg-white dark:bg-[#141414] px-3 py-2 text-sm text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900"
              />
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
