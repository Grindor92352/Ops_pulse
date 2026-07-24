package com.opspulse.dto;

import com.opspulse.entity.PlanType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class ProjectDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateProjectRequest {
        @NotBlank(message = "Project name is required")
        private String name;
        private String description;
        private List<String> techStack;
        private PlanType plan = PlanType.BASIC;
        private String githubRepoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectResponse {
        private String id;
        private String name;
        private String description;
        private List<String> techStack;
        private PlanType plan;
        private String sdkApiKey;
        private String githubRepoUrl;
        private String orgId;
        private LocalDateTime createdAt;
    }
}
