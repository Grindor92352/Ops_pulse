package com.opspulse.controller;

import com.opspulse.dto.ApiResponse;
import com.opspulse.dto.DashboardDtos.DashboardStatsResponse;
import com.opspulse.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Real-time Operations Telemetry & Dashboard Stats")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get telemetry metrics, SLA compliance rates, and recent incident logs")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(
            @RequestParam(required = false) String projectId) {
        DashboardStatsResponse stats = dashboardService.getStats(projectId);
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched successfully", stats));
    }
}
