package com.opspulse.dto;

import com.opspulse.dto.IssueDtos.IssueResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class DashboardDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStatsResponse {
        private long totalIssues;
        private long openIssues;
        private long inProgressIssues;
        private long resolvedIssues;

        private long criticalIssues;
        private long highIssues;
        private long mediumIssues;
        private long lowIssues;

        private long responseSlaBreaches;
        private long resolutionSlaBreaches;
        private double slaComplianceRate;

        private Map<String, Long> severityBreakdown;
        private Map<String, Long> statusBreakdown;
        private List<IssueResponse> recentIssues;
    }
}
