package com.opspulse.service;

import com.opspulse.dto.IssueDtos.*;
import com.opspulse.entity.*;
import com.opspulse.exception.ResourceNotFoundException;
import com.opspulse.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final IssueActivityRepository activityRepository;
    private final IssueCommentRepository commentRepository;
    private final SlaService slaService;

    @Transactional
    public IssueResponse createIssue(CreateIssueRequest request, String userId) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SlaService.SlaDeadlines deadlines = slaService.calculateDeadlines(request.getSeverity(), project.getPlan());

        Issue issue = Issue.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .source(IssueSource.MANUAL)
                .severity(request.getSeverity())
                .priority(request.getPriority() != null ? request.getPriority() : IssuePriority.MEDIUM)
                .status(IssueStatus.OPEN)
                .environment(request.getEnvironment() != null ? request.getEnvironment() : EnvironmentType.PRODUCTION)
                .project(project)
                .logs(request.getLogs())
                .responseSlaDeadline(deadlines.getResponseDeadline())
                .resolutionSlaDeadline(deadlines.getResolutionDeadline())
                .build();

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId()).orElse(null);
            if (team != null) {
                issue.setTeam(team);
            }
        }

        if (request.getAssignedToId() != null) {
            User assignedUser = userRepository.findById(request.getAssignedToId()).orElse(null);
            if (assignedUser != null) {
                issue.setAssignedTo(assignedUser);
                issue.setStatus(IssueStatus.ASSIGNED);
                issue.setAcceptedAt(LocalDateTime.now());
            }
        }

        Issue savedIssue = issueRepository.save(issue);

        logActivity(savedIssue, user, "Issue created manually");

        return mapToResponse(savedIssue);
    }

    @Transactional
    public IssueResponse updateIssue(String issueId, UpdateIssueRequest request, String userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getStatus() != null && request.getStatus() != issue.getStatus()) {
            IssueStatus oldStatus = issue.getStatus();
            issue.setStatus(request.getStatus());
            logActivity(issue, user, "Status updated from " + oldStatus + " to " + request.getStatus());

            if (request.getStatus() == IssueStatus.RESOLVED) {
                issue.setResolvedAt(LocalDateTime.now());
            }
            if ((request.getStatus() == IssueStatus.ASSIGNED || request.getStatus() == IssueStatus.IN_PROGRESS) && issue.getAcceptedAt() == null) {
                issue.setAcceptedAt(LocalDateTime.now());
            }
        }

        if (request.getAssignedToId() != null) {
            User assignedUser = userRepository.findById(request.getAssignedToId()).orElse(null);
            if (assignedUser != null) {
                issue.setAssignedTo(assignedUser);
                logActivity(issue, user, "Assigned to " + assignedUser.getEmail());
            }
        }

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId()).orElse(null);
            if (team != null) {
                issue.setTeam(team);
                logActivity(issue, user, "Assigned to team " + team.getName());
            }
        }

        if (request.getRootCause() != null) {
            issue.setRootCause(request.getRootCause());
        }

        Issue savedIssue = issueRepository.save(issue);
        return mapToResponse(savedIssue);
    }

    @Transactional
    public CommentDto addComment(String issueId, String text, String userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        IssueComment comment = IssueComment.builder()
                .issue(issue)
                .user(user)
                .text(text)
                .build();

        comment = commentRepository.save(comment);
        logActivity(issue, user, "Comment added");

        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssueById(String issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));
        return mapToResponse(issue);
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> getIssuesByProject(String projectId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return issueRepository.findByProjectId(projectId, pageRequest)
                .map(this::mapToResponse);
    }

    private void logActivity(Issue issue, User user, String action) {
        IssueActivity activity = IssueActivity.builder()
                .issue(issue)
                .user(user)
                .action(action)
                .build();
        activityRepository.save(activity);
    }

    public IssueResponse mapToResponse(Issue issue) {
        List<ActivityDto> activities = activityRepository.findByIssueIdOrderByCreatedAtDesc(issue.getId()).stream()
                .map(a -> ActivityDto.builder()
                        .id(a.getId())
                        .action(a.getAction())
                        .userName(a.getUser() != null ? a.getUser().getName() : "System")
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<CommentDto> comments = commentRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId()).stream()
                .map(c -> CommentDto.builder()
                        .id(c.getId())
                        .text(c.getText())
                        .userName(c.getUser().getName())
                        .userEmail(c.getUser().getEmail())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return IssueResponse.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .source(issue.getSource().name())
                .severity(issue.getSeverity())
                .priority(issue.getPriority())
                .status(issue.getStatus())
                .environment(issue.getEnvironment())
                .projectId(issue.getProject() != null ? issue.getProject().getId() : null)
                .projectName(issue.getProject() != null ? issue.getProject().getName() : null)
                .assignedToId(issue.getAssignedTo() != null ? issue.getAssignedTo().getId() : null)
                .assignedToEmail(issue.getAssignedTo() != null ? issue.getAssignedTo().getEmail() : null)
                .teamId(issue.getTeam() != null ? issue.getTeam().getId() : null)
                .teamName(issue.getTeam() != null ? issue.getTeam().getName() : null)
                .logs(issue.getLogs())
                .rootCause(issue.getRootCause())
                .suggestedFixes(issue.getSuggestedFixes())
                .responseSlaDeadline(issue.getResponseSlaDeadline())
                .resolutionSlaDeadline(issue.getResolutionSlaDeadline())
                .responseBreached(issue.getResponseBreached())
                .resolutionBreached(issue.getResolutionBreached())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .activities(activities)
                .comments(comments)
                .build();
    }
}
