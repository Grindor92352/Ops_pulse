package com.opspulse.controller;

import com.opspulse.dto.ApiResponse;
import com.opspulse.entity.*;
import com.opspulse.repository.IssueActivityRepository;
import com.opspulse.repository.IssueRepository;
import com.opspulse.repository.ProjectRepository;
import com.opspulse.service.AiAnalysisService;
import com.opspulse.service.SlaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "GitHub CI/CD and Pull Request Webhook Ingestion")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Value("${opspulse.github.webhook-secret:opspulse_github_secret_12345}")
    private String webhookSecret;

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final IssueActivityRepository activityRepository;
    private final SlaService slaService;
    private final AiAnalysisService aiAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/github")
    @Operation(summary = "GitHub Webhook Event Handler (CI/CD failures & PR events)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGitHubWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event) {

        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (signature == null || !verifySignature(rawPayload, signature, webhookSecret)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid GitHub Webhook Signature"));
            }
        }

        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            String repoUrl = payload.path("repository").path("html_url").asText(null);
            String action = payload.path("action").asText("");

            Optional<Project> optionalProject = Optional.empty();
            if (repoUrl != null) {
                optionalProject = projectRepository.findAll().stream()
                        .filter(p -> p.getGithubRepoUrl() != null && normalizeRepoUrl(p.getGithubRepoUrl()).equalsIgnoreCase(normalizeRepoUrl(repoUrl)))
                        .findFirst();
            }

            if (optionalProject.isEmpty()) {
                List<Project> allProjects = projectRepository.findAll();
                if (!allProjects.isEmpty()) {
                    optionalProject = Optional.of(allProjects.get(0));
                }
            }

            if (optionalProject.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("Webhook received, but no linked project found", Map.of("processed", false)));
            }

            Project project = optionalProject.get();

            boolean created = false;
            String incidentTitle = "";
            String incidentUrl = "";

            if ("workflow_run".equals(event) && "completed".equals(action)) {
                String conclusion = payload.path("workflow_run").path("conclusion").asText("");
                if (isFailedConclusion(conclusion)) {
                    incidentTitle = "CI Failure: " + payload.path("workflow_run").path("name").asText("Workflow");
                    incidentUrl = payload.path("workflow_run").path("html_url").asText("");
                    created = true;
                }
            } else if ("check_run".equals(event) && "completed".equals(action)) {
                String conclusion = payload.path("check_run").path("conclusion").asText("");
                if (isFailedConclusion(conclusion)) {
                    incidentTitle = "CI Check Failure: " + payload.path("check_run").path("name").asText("Check Run");
                    incidentUrl = payload.path("check_run").path("html_url").asText("");
                    created = true;
                }
            } else if ("pull_request".equals(event)) {
                JsonNode pr = payload.path("pull_request");
                String mergeableState = pr.path("mergeable_state").asText("");
                if ("dirty".equals(mergeableState)) {
                    incidentTitle = "MERGE CONFLICT: PR #" + pr.path("number").asInt();
                    incidentUrl = pr.path("html_url").asText("");
                    created = true;
                }
            }

            if (created) {
                Issue issue = Issue.builder()
                        .title(incidentTitle)
                        .description("GitHub Incident reported from URL: " + incidentUrl)
                        .source(IssueSource.GITHUB)
                        .severity(IssueSeverity.HIGH)
                        .priority(IssuePriority.HIGH)
                        .status(IssueStatus.OPEN)
                        .environment(EnvironmentType.PRODUCTION)
                        .project(project)
                        .logs(rawPayload)
                        .build();

                issue = issueRepository.save(issue);

                IssueActivity activity = IssueActivity.builder()
                        .issue(issue)
                        .action("GitHub Webhook incident created for event: " + event)
                        .build();
                activityRepository.save(activity);

                processAiAnalysisAsync(issue.getId(), rawPayload, project);

                return ResponseEntity.ok(ApiResponse.success("GitHub incident created successfully", Map.of(
                        "created", true,
                        "issueId", issue.getId(),
                        "title", incidentTitle
                )));
            }

            return ResponseEntity.ok(ApiResponse.success("GitHub event received and evaluated (no action required)", Map.of("processed", true)));

        } catch (Exception ex) {
            logger.error("Error processing GitHub webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process GitHub webhook: " + ex.getMessage()));
        }
    }

    @Async
    public void processAiAnalysisAsync(String issueId, String rawPayload, Project project) {
        try {
            Map<String, Object> rawData = objectMapper.readValue(rawPayload, Map.class);
            var aiResult = aiAnalysisService.analyzeIncident(rawData, IssueSource.GITHUB, project.getTechStack());
            var deadlines = slaService.calculateDeadlines(aiResult.getSeverity(), project.getPlan());

            issueRepository.findById(issueId).ifPresent(issue -> {
                issue.setTitle(aiResult.getTitle());
                issue.setDescription(aiResult.getDescription());
                issue.setRootCause(aiResult.getRootCause());
                issue.setSuggestedFixes(aiResult.getSuggestedFixes());
                issue.setSeverity(aiResult.getSeverity());
                issue.setPriority(aiResult.getPriority());
                issue.setResponseSlaDeadline(deadlines.getResponseDeadline());
                issue.setResolutionSlaDeadline(deadlines.getResolutionDeadline());
                issueRepository.save(issue);
            });
        } catch (Exception e) {
            logger.error("Async AI analysis for GitHub webhook failed: {}", e.getMessage());
        }
    }

    private boolean isFailedConclusion(String conclusion) {
        return "failure".equalsIgnoreCase(conclusion) ||
                "timed_out".equalsIgnoreCase(conclusion) ||
                "cancelled".equalsIgnoreCase(conclusion) ||
                "action_required".equalsIgnoreCase(conclusion);
    }

    private String normalizeRepoUrl(String url) {
        if (url == null) return "";
        return url.toLowerCase().replace("https://github.com/", "").replace(".git", "").trim();
    }

    private boolean verifySignature(String payload, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexHash = new StringBuilder("sha256=");
            for (byte b : hash) {
                hexHash.append(String.format("%02x", b));
            }
            return MessageDigest.isEqual(hexHash.toString().getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
