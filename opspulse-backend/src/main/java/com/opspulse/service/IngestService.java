package com.opspulse.service;

import com.opspulse.dto.AiAnalysisResultDto;
import com.opspulse.dto.IngestTelemetryDto;
import com.opspulse.dto.IssueDtos.IssueResponse;
import com.opspulse.entity.*;
import com.opspulse.exception.UnauthorizedException;
import com.opspulse.repository.IssueActivityRepository;
import com.opspulse.repository.IssueRepository;
import com.opspulse.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IngestService {

    private static final Logger logger = LoggerFactory.getLogger(IngestService.class);

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final IssueActivityRepository activityRepository;
    private final SlaService slaService;
    private final AiAnalysisService aiAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public IssueResponse ingestTelemetry(IngestTelemetryDto dto) {
        if (dto.getApiKey() == null || dto.getApiKey().isBlank()) {
            throw new UnauthorizedException("SDK API Key is required");
        }

        Project project = projectRepository.findBySdkApiKey(dto.getApiKey())
                .orElseThrow(() -> new UnauthorizedException("Invalid SDK API Key"));

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("message", dto.getMessage());
        rawData.put("name", dto.getName());
        rawData.put("stack", dto.getStack());
        rawData.put("url", dto.getUrl());
        rawData.put("userAgent", dto.getUserAgent());
        rawData.put("breadcrumbs", dto.getBreadcrumbs());
        rawData.put("environment", dto.getEnvironment());

        AiAnalysisResultDto aiResult = aiAnalysisService.analyzeIncident(rawData, IssueSource.SDK, project.getTechStack());

        SlaService.SlaDeadlines deadlines = slaService.calculateDeadlines(aiResult.getSeverity(), project.getPlan());

        String rawLogsString;
        try {
            rawLogsString = objectMapper.writeValueAsString(rawData);
        } catch (Exception e) {
            rawLogsString = rawData.toString();
        }

        Issue issue = Issue.builder()
                .title(aiResult.getTitle())
                .description(aiResult.getDescription())
                .source(IssueSource.SDK)
                .severity(aiResult.getSeverity())
                .priority(aiResult.getPriority())
                .status(IssueStatus.OPEN)
                .environment(aiResult.getEnvironment())
                .project(project)
                .rootCause(aiResult.getRootCause())
                .suggestedFixes(aiResult.getSuggestedFixes())
                .logs(rawLogsString)
                .responseSlaDeadline(deadlines.getResponseDeadline())
                .resolutionSlaDeadline(deadlines.getResolutionDeadline())
                .build();

        issue = issueRepository.save(issue);

        IssueActivity activity = IssueActivity.builder()
                .issue(issue)
                .action("SDK error captured and AI root-cause analysis completed.")
                .build();
        activityRepository.save(activity);

        logger.info("Successfully ingested SDK telemetry incident ID: {} for project: {}", issue.getId(), project.getName());

        return IssueResponse.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .source(issue.getSource().name())
                .severity(issue.getSeverity())
                .priority(issue.getPriority())
                .status(issue.getStatus())
                .environment(issue.getEnvironment())
                .projectId(project.getId())
                .projectName(project.getName())
                .rootCause(issue.getRootCause())
                .suggestedFixes(issue.getSuggestedFixes())
                .responseSlaDeadline(issue.getResponseSlaDeadline())
                .resolutionSlaDeadline(issue.getResolutionSlaDeadline())
                .createdAt(issue.getCreatedAt())
                .build();
    }
}
