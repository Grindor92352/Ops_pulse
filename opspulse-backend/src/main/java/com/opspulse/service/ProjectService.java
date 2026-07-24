package com.opspulse.service;

import com.opspulse.dto.ProjectDtos.*;
import com.opspulse.entity.Organization;
import com.opspulse.entity.Project;
import com.opspulse.entity.User;
import com.opspulse.exception.BadRequestException;
import com.opspulse.exception.ResourceNotFoundException;
import com.opspulse.repository.OrganizationRepository;
import com.opspulse.repository.ProjectRepository;
import com.opspulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, String userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organization org = creator.getOrganization();
        if (org == null) {
            org = Organization.builder().name(creator.getName() + "'s Org").build();
            org = organizationRepository.save(org);
            creator.setOrganization(org);
            userRepository.save(creator);
        }

        String apiKey = "sdk_" + UUID.randomUUID().toString().replace("-", "");

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .techStack(request.getTechStack() != null ? request.getTechStack() : List.of("Java", "React"))
                .plan(request.getPlan() != null ? request.getPlan() : org.getPlan())
                .githubRepoUrl(request.getGithubRepoUrl())
                .sdkApiKey(apiKey)
                .organization(org)
                .creator(creator)
                .build();

        project = projectRepository.save(project);
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByOrg(String orgId) {
        return projectRepository.findByOrganizationId(orgId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public String regenerateApiKey(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        String newKey = "sdk_" + UUID.randomUUID().toString().replace("-", "");
        project.setSdkApiKey(newKey);
        projectRepository.save(project);
        return newKey;
    }

    public ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .techStack(project.getTechStack())
                .plan(project.getPlan())
                .sdkApiKey(project.getSdkApiKey())
                .githubRepoUrl(project.getGithubRepoUrl())
                .orgId(project.getOrganization() != null ? project.getOrganization().getId() : null)
                .createdAt(project.getCreatedAt())
                .build();
    }
}
