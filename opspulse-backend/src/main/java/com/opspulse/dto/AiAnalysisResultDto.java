package com.opspulse.dto;

import com.opspulse.entity.EnvironmentType;
import com.opspulse.entity.IssuePriority;
import com.opspulse.entity.IssueSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResultDto {
    private String title;
    private String description;
    private IssueSeverity severity;
    private IssuePriority priority;
    private EnvironmentType environment;
    private String rootCause;
    private String suggestedFixes;
}
