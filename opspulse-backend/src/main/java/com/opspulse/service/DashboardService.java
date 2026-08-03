package com.opspulse.service;

import com.opspulse.dto.DashboardDtos.DashboardStatsResponse;
import com.opspulse.dto.IssueDtos.IssueResponse;
import com.opspulse.entity.IssueSeverity;
import com.opspulse.entity.IssueStatus;
import com.opspulse.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final IssueRepository issueRepository;
    private final IssueService issueService;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(String projectId) {
        if (projectId != null && !projectId.isBlank()) {
            return getProjectStats(projectId);
        } else {
            return getGlobalStats();
        }
    }

    private DashboardStatsResponse getGlobalStats() {
        long total = issueRepository.count();
        long open = issueRepository.countByStatus(IssueStatus.OPEN);
        long inProgress = issueRepository.countByStatus(IssueStatus.IN_PROGRESS);
        long resolved = issueRepository.countByStatus(IssueStatus.RESOLVED);

        long critical = issueRepository.countBySeverity(IssueSeverity.CRITICAL);
        long high = issueRepository.countBySeverity(IssueSeverity.HIGH);
        long medium = issueRepository.countBySeverity(IssueSeverity.MEDIUM);
        long low = issueRepository.countBySeverity(IssueSeverity.LOW);

        long respBreaches = issueRepository.countByResponseBreachedTrue();
        long resBreaches = issueRepository.countByResolutionBreachedTrue();

        double complianceRate = total > 0 ? Math.max(0, 100.0 - (((double) (respBreaches + resBreaches) / (total * 2.0)) * 100.0)) : 100.0;

        List<IssueResponse> recent = issueRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(issueService::mapToResponse)
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .totalIssues(total)
                .openIssues(open)
                .inProgressIssues(inProgress)
                .resolvedIssues(resolved)
                .criticalIssues(critical)
                .highIssues(high)
                .mediumIssues(medium)
                .lowIssues(low)
                .responseSlaBreaches(respBreaches)
                .resolutionSlaBreaches(resBreaches)
                .slaComplianceRate(Math.round(complianceRate * 10.0) / 10.0)
                .severityBreakdown(Map.of(
                        "CRITICAL", critical,
                        "HIGH", high,
                        "MEDIUM", medium,
                        "LOW", low
                ))
                .statusBreakdown(Map.of(
                        "OPEN", open,
                        "IN_PROGRESS", inProgress,
                        "RESOLVED", resolved
                ))
                .recentIssues(recent)
                .build();
    }

    private DashboardStatsResponse getProjectStats(String projectId) {
        long total = issueRepository.countByProjectId(projectId);
        long open = issueRepository.countByProjectIdAndStatus(projectId, IssueStatus.OPEN);
        long inProgress = issueRepository.countByProjectIdAndStatus(projectId, IssueStatus.IN_PROGRESS);
        long resolved = issueRepository.countByProjectIdAndStatus(projectId, IssueStatus.RESOLVED);

        long critical = issueRepository.countByProjectIdAndSeverity(projectId, IssueSeverity.CRITICAL);
        long high = issueRepository.countByProjectIdAndSeverity(projectId, IssueSeverity.HIGH);
        long medium = issueRepository.countByProjectIdAndSeverity(projectId, IssueSeverity.MEDIUM);
        long low = issueRepository.countByProjectIdAndSeverity(projectId, IssueSeverity.LOW);

        long respBreaches = issueRepository.countByProjectIdAndResponseBreachedTrue(projectId);
        long resBreaches = issueRepository.countByProjectIdAndResolutionBreachedTrue(projectId);

        double complianceRate = total > 0 ? Math.max(0, 100.0 - (((double) (respBreaches + resBreaches) / (total * 2.0)) * 100.0)) : 100.0;

        List<IssueResponse> recent = issueRepository.findTop10ByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(issueService::mapToResponse)
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .totalIssues(total)
                .openIssues(open)
                .inProgressIssues(inProgress)
                .resolvedIssues(resolved)
                .criticalIssues(critical)
                .highIssues(high)
                .mediumIssues(medium)
                .lowIssues(low)
                .responseSlaBreaches(respBreaches)
                .resolutionSlaBreaches(resBreaches)
                .slaComplianceRate(Math.round(complianceRate * 10.0) / 10.0)
                .severityBreakdown(Map.of(
                        "CRITICAL", critical,
                        "HIGH", high,
                        "MEDIUM", medium,
                        "LOW", low
                ))
                .statusBreakdown(Map.of(
                        "OPEN", open,
                        "IN_PROGRESS", inProgress,
                        "RESOLVED", resolved
                ))
                .recentIssues(recent)
                .build();
    }
}
