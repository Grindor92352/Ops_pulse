package com.opspulse.service;

import com.opspulse.entity.IssueSeverity;
import com.opspulse.entity.PlanType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SlaService {

    public static class SlaDeadlines {
        private final LocalDateTime responseDeadline;
        private final LocalDateTime resolutionDeadline;

        public SlaDeadlines(LocalDateTime responseDeadline, LocalDateTime resolutionDeadline) {
            this.responseDeadline = responseDeadline;
            this.resolutionDeadline = resolutionDeadline;
        }

        public LocalDateTime getResponseDeadline() {
            return responseDeadline;
        }

        public LocalDateTime getResolutionDeadline() {
            return resolutionDeadline;
        }
    }

    public SlaDeadlines calculateDeadlines(IssueSeverity severity, PlanType plan) {
        if (plan == null || plan == PlanType.BASIC) {
            return new SlaDeadlines(null, null);
        }

        LocalDateTime now = LocalDateTime.now();
        long responseHours = 24;
        long resolutionHours = 24 * 7;

        switch (severity) {
            case CRITICAL:
                responseHours = 1;
                resolutionHours = 4;
                break;
            case HIGH:
                responseHours = 4;
                resolutionHours = 24;
                break;
            case MEDIUM:
                responseHours = 8;
                resolutionHours = 24 * 3;
                break;
            case LOW:
                responseHours = 24;
                resolutionHours = 24 * 7;
                break;
        }

        return new SlaDeadlines(
                now.plusHours(responseHours),
                now.plusHours(resolutionHours)
        );
    }
}
