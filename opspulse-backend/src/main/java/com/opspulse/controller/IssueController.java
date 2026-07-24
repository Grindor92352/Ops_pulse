package com.opspulse.controller;

import com.opspulse.dto.ApiResponse;
import com.opspulse.dto.IssueDtos.*;
import com.opspulse.security.UserPrincipal;
import com.opspulse.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Tag(name = "Issues", description = "Incident Management, SLA Tracking & Activity Feed")
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    @Operation(summary = "Create an incident manually")
    public ResponseEntity<ApiResponse<IssueResponse>> createIssue(@Valid @RequestBody CreateIssueRequest request,
                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        IssueResponse response = issueService.createIssue(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Issue created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update issue status, assignment, or root cause")
    public ResponseEntity<ApiResponse<IssueResponse>> updateIssue(@PathVariable String id,
                                                                   @RequestBody UpdateIssueRequest request,
                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        IssueResponse response = issueService.updateIssue(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Issue updated successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full incident details with timeline and comments")
    public ResponseEntity<ApiResponse<IssueResponse>> getIssueById(@PathVariable String id) {
        IssueResponse response = issueService.getIssueById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get paginated incidents for a project")
    public ResponseEntity<ApiResponse<Page<IssueResponse>>> getIssuesByProject(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<IssueResponse> response = issueService.getIssuesByProject(projectId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add comment to an incident")
    public ResponseEntity<ApiResponse<CommentDto>> addComment(@PathVariable String id,
                                                               @Valid @RequestBody AddCommentRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        CommentDto response = issueService.addComment(id, request.getText(), principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Comment added", response));
    }
}
