package com.yourco.saas.selfservice.dto;

import com.yourco.saas.collaboration.dto.CalendarEventResponse;
import com.yourco.saas.collaboration.dto.NotificationResponse;
import com.yourco.saas.leave.dto.LeaveBalanceResponse;
import com.yourco.saas.leave.dto.LeaveRequestResponse;
import com.yourco.saas.projects.dto.ProjectResponse;
import com.yourco.saas.projects.dto.TaskResponse;

import java.util.List;

public record SelfServiceOverviewDto(
        SelfServiceProfileDto profile,
        List<TaskResponse> assignedTasks,
        List<ProjectResponse> assignedProjects,
        List<LeaveBalanceResponse> leaveBalances,
        List<LeaveRequestResponse> recentLeaveRequests,
        List<NotificationResponse> recentNotifications,
        List<CalendarEventResponse> upcomingEvents
) {}
