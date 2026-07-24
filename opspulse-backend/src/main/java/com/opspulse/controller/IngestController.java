package com.opspulse.controller;

import com.opspulse.dto.ApiResponse;
import com.opspulse.dto.IngestTelemetryDto;
import com.opspulse.dto.IssueDtos.IssueResponse;
import com.opspulse.service.IngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
@Tag(name = "Telemetry Ingestion", description = "Public SDK endpoint for real-time error streaming")
public class IngestController {

    private final IngestService ingestService;

    @PostMapping
    @Operation(summary = "Ingest client SDK telemetry payload and trigger AI analysis")
    public ResponseEntity<ApiResponse<IssueResponse>> ingestTelemetry(@RequestBody IngestTelemetryDto dto) {
        IssueResponse response = ingestService.ingestTelemetry(dto);
        return ResponseEntity.ok(ApiResponse.success("Telemetry processed and AI root-cause analyzed", response));
    }
}
