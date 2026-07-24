package com.opspulse.controller;

import com.opspulse.dto.ApiResponse;
import com.opspulse.dto.ProjectDtos.*;
import com.opspulse.security.UserPrincipal;
import com.opspulse.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project Management & SDK Key generation")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create a new monitored project")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody CreateProjectRequest request,
                                                                       @AuthenticationPrincipal UserPrincipal principal) {
        ProjectResponse response = projectService.createProject(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Project created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project details by ID")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(@PathVariable String id) {
        ProjectResponse response = projectService.getProjectById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/org/{orgId}")
    @Operation(summary = "List all projects in an organization")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjectsByOrg(@PathVariable String orgId) {
        List<ProjectResponse> projects = projectService.getProjectsByOrg(orgId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @PostMapping("/{id}/api-key")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Regenerate SDK tracking API Key")
    public ResponseEntity<ApiResponse<Map<String, String>>> regenerateApiKey(@PathVariable String id) {
        String newKey = projectService.regenerateApiKey(id);
        return ResponseEntity.ok(ApiResponse.success("API Key regenerated", Map.of("apiKey", newKey)));
    }
}
