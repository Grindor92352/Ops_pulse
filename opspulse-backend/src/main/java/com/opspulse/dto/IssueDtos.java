package com.opspulse.dto;

import com.opspulse.entity.EnvironmentType;
import com.opspulse.entity.IssuePriority;
import com.opspulse.entity.IssueSeverity;
import com.opspulse.entity.IssueStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class IssueDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateIssueRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Description is required")
        private String description;

        @NotBlank(message = "ProjectId is required")
        private String projectId;

        private IssueSeverity severity = IssueSeverity.LOW;
        private IssuePriority priority = IssuePriority.MEDIUM;
        private EnvironmentType environment = EnvironmentType.PRODUCTION;
        private String teamId;
        private String assignedToId;
        private String logs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateIssueRequest {
        private IssueStatus status;
        private String teamId;
        private String assignedToId;
        private String rootCause;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddCommentRequest {
        @NotBlank(message = "Comment text cannot be empty")
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueResponse {
        private String id;
        private String title;
        private String description;
        private String source;
        private IssueSeverity severity;
        private IssuePriority priority;
        private IssueStatus status;
        private EnvironmentType environment;
        private String projectId;
        private String projectName;
        private String assignedToId;
        private String assignedToEmail;
        private String teamId;
        private String teamName;
        private String logs;
        private String rootCause;
        private String suggestedFixes;
        private LocalDateTime responseSlaDeadline;
        private LocalDateTime resolutionSlaDeadline;
        private Boolean responseBreached;
        private Boolean resolutionBreached;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ActivityDto> activities;
        private List<CommentDto> comments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDto {
        private String id;
        private String action;
        private String userName;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentDto {
        private String id;
        private String text;
        private String userName;
        private String userEmail;
        private LocalDateTime createdAt;
    }
}
